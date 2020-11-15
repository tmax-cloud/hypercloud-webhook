package main

import (
	"context"
	"crypto/tls"
	"encoding/json"
	"flag"
	"fmt"
	"io/ioutil"
	"net/http"
	"os"
	"os/signal"
	"syscall"

	"k8s.io/klog"

	"k8s.io/api/admission/v1beta1"

	admission "hypercloud4-webhook/admission"
	audit "hypercloud4-webhook/audit"
	util "hypercloud4-webhook/util"
)

type admitFunc func(v1beta1.AdmissionReview) *v1beta1.AdmissionResponse

func serve(w http.ResponseWriter, r *http.Request, admit admitFunc) {
	var body []byte
	if r.Body != nil {
		if data, err := ioutil.ReadAll(r.Body); err == nil {
			body = data
		}
	}
	klog.Infof("Request body: %s\n", body)

	contentType := r.Header.Get("Content-Type")
	if contentType != "application/json" {
		klog.Errorf("contentType=%s, expect application/json", contentType)
		return
	}

	requestedAdmissionReview := v1beta1.AdmissionReview{}
	responseAdmissionReview := v1beta1.AdmissionReview{}

	if err := json.Unmarshal(body, &requestedAdmissionReview); err != nil {
		klog.Error(err)
		responseAdmissionReview.Response = util.ToAdmissionResponse(err)
	} else {
		responseAdmissionReview.Response = admit(requestedAdmissionReview)
	}

	responseAdmissionReview.Response.UID = requestedAdmissionReview.Request.UID

	respBytes, err := json.Marshal(responseAdmissionReview)

	klog.Infof("Response body: %s\n", respBytes)

	if err != nil {
		klog.Error(err)
		responseAdmissionReview.Response = util.ToAdmissionResponse(err)
	}
	if _, err := w.Write(respBytes); err != nil {
		klog.Error(err)
		responseAdmissionReview.Response = util.ToAdmissionResponse(err)
	}
}

func serveMetadata(w http.ResponseWriter, r *http.Request) {
	klog.Infof("Http request: method=%s, uri=%s", r.Method, r.URL.Path)
	serve(w, r, admission.AddResourceMeta)
}

func serveSidecarInjection(w http.ResponseWriter, r *http.Request) {
	klog.Infof("Http request: method=%s, uri=%s", r.Method, r.URL.Path)
	serve(w, r, admission.SidecarInjection)
}

func serveAudit(w http.ResponseWriter, r *http.Request) {
	klog.Infof("Http request: method=%s, uri=%s", r.Method, r.URL.Path)
	switch r.Method {
	case http.MethodGet:
		audit.GetAudit(w, r)
	case http.MethodPost:
		audit.AddAudit(w, r)
	case http.MethodPut:
	case http.MethodDelete:
	default:
		//error
	}
}

func serveAuditBatch(w http.ResponseWriter, r *http.Request) {
	klog.Infof("Http request: method=%s, uri=%s", r.Method, r.URL.Path)
	audit.AddAuditBatch(w, r)
}

func serveAuditWss(w http.ResponseWriter, r *http.Request) {
	klog.Infof("Http request: method=%s, uri=%s", r.Method, r.URL.Path)
	audit.ServeWss(w, r)
}

func serveTest(w http.ResponseWriter, r *http.Request) {
	klog.Infof("Http request: method=%s, uri=%s", r.Method, r.URL.Path)
	var body []byte
	if r.Body != nil {
		if data, err := ioutil.ReadAll(r.Body); err == nil {
			body = data
		}
	}
	klog.Info("Request body: \n", string(body))
}

var (
	port     int
	certFile string
	keyFile  string
)

func main() {
	flag.IntVar(&port, "port", 8443, "hypercloud-webhook server port")
	flag.StringVar(&certFile, "certFile", "/run/secrets/tls/server.crt", "hypercloud-webhook server cert")
	flag.StringVar(&keyFile, "keyFile", "/run/secrets/tls/server.key", "x509 Private key file for TLS connection")
	flag.StringVar(&admission.SidecarContainerImage, "sidecarImage", "fluent/fluent-bit:1.5-debug", "Fluent-bit image name.")
	flag.Parse()

	keyPair, err := tls.LoadX509KeyPair(certFile, keyFile)
	if err != nil {
		klog.Errorf("Failed to load key pair: %s", err)
	}

	mux := http.NewServeMux()
	mux.HandleFunc("/api/webhook/metadata", serveMetadata)
	mux.HandleFunc("/api/webhook/audit", serveAudit)
	mux.HandleFunc("/api/webhook/audit/batch", serveAuditBatch)
	mux.HandleFunc("/api/webhook/audit/websocket", serveAuditWss)
	mux.HandleFunc("/api/webhook/inject", serveSidecarInjection)
	mux.HandleFunc("/api/webhook/test", serveTest)

	whsvr := &http.Server{
		Addr:      fmt.Sprintf(":%d", port),
		Handler:   mux,
		TLSConfig: &tls.Config{Certificates: []tls.Certificate{keyPair}},
	}

	klog.Info("Starting webhook server...")

	go func() {
		// if err := whsvr.ListenAndServe(); err != nil { //HTTPS로 서버 시작
		if err := whsvr.ListenAndServeTLS("", ""); err != nil { //HTTPS로 서버 시작
			klog.Errorf("Failed to listen and serve webhook server: %s", err)
		}
	}()

	sigCh := make(chan os.Signal, 1)
	signal.Notify(sigCh, syscall.SIGINT, syscall.SIGTERM)
	<-sigCh

	klog.Info("OS shutdown signal received...")
	whsvr.Shutdown(context.Background())
}
