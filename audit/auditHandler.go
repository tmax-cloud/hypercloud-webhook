package audit

import (
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net/http"
	"strconv"
	"strings"
	"time"

	"hypercloud4-webhook/util"

	"github.com/google/uuid"
	types "k8s.io/apimachinery/pkg/types"
	"k8s.io/apiserver/pkg/apis/audit"
	"k8s.io/klog"
)

type urlParam struct {
	Namespace string   `json:"namespace"`
	Resource  string   `json:"resource"`
	StartTime string   `json:"startTime"`
	EndTime   string   `json:"endTime"`
	Limit     string   `json:"limit"`
	Offset    string   `json:"offset"`
	Code      string   `json:"code"`
	Verb      string   `json:"verb"`
	Status    string   `json:"status"`
	Sort      []string `json:"sort"`
}

type response struct {
	EventList audit.EventList `json:"eventList"`
	RowsCount int64           `json:"rowsCount"`
}

func AddAudit(w http.ResponseWriter, r *http.Request) {
	var body []byte
	var eventList audit.EventList

	if r.Body != nil {
		if data, err := ioutil.ReadAll(r.Body); err == nil {
			body = data
		}
	}

	if err := json.Unmarshal(body, &eventList); err != nil {
		util.SetResponse(w, "", nil, http.StatusInternalServerError)
	}

	contentType := r.Header.Get("Content-Type")
	if contentType != "application/json" {
		klog.Errorf("contentType=%s, expect application/json", contentType)
		return
	}

	for _, event := range eventList.Items {
		if event.ResponseStatus.Status == "" && (event.ResponseStatus.Code/100) == 2 {
			event.ResponseStatus.Status = "Success"
		}
	}

	insert(eventList.Items)
	if len(hub.clients) > 0 {
		hub.broadcast <- eventList
	}
	util.SetResponse(w, "", nil, http.StatusOK)
}

func AddAuditBatch(w http.ResponseWriter, r *http.Request) {
	klog.Info("AddAuditBatch")
	var body []byte
	var event audit.Event
	if r.Body != nil {
		if data, err := ioutil.ReadAll(r.Body); err == nil {
			body = data
		}
	}

	if err := json.Unmarshal(body, &event); err != nil {
		klog.Error(err)
		util.SetResponse(w, "", nil, http.StatusInternalServerError)
	}

	contentType := r.Header.Get("Content-Type")
	if contentType != "application/json" {
		klog.Errorf("contentType=%s, expect application/json", contentType)
		return
	}

	event.AuditID = types.UID(uuid.New().String())
	if event.StageTimestamp.Time.IsZero() {
		event.StageTimestamp.Time = time.Now()
	}

	if len(eventBuffer.buffer) < bufferSize {
		eventBuffer.buffer <- event
	} else {
		klog.Error("event is dropped.")
	}
	util.SetResponse(w, "", nil, http.StatusOK)
}

func GetAudit(w http.ResponseWriter, r *http.Request) {
	urlParam := urlParam{}

	urlParam.Namespace = r.URL.Query().Get("namespace")
	urlParam.Resource = r.URL.Query().Get("resource")
	urlParam.Limit = r.URL.Query().Get("limit")
	urlParam.Offset = r.URL.Query().Get("offset")
	urlParam.Code = r.URL.Query().Get("code")
	urlParam.Verb = r.URL.Query().Get("verb")
	urlParam.Sort = r.URL.Query()["sort"]
	urlParam.StartTime = r.URL.Query().Get("startTime")
	urlParam.EndTime = r.URL.Query().Get("endTime")
	urlParam.Status = r.URL.Query().Get("status")

	query := queryBuilder(urlParam)

	eventList, count := get(query)

	response := response{
		EventList: eventList,
		RowsCount: count,
	}

	util.SetResponse(w, "", response, http.StatusOK)
}

func queryBuilder(param urlParam) string {

	namespace := param.Namespace
	resource := param.Resource
	startTime := param.StartTime
	endTime := param.EndTime
	limit := param.Limit
	offset := param.Offset
	code := param.Code
	verb := param.Verb
	sort := param.Sort
	status := param.Status

	var b strings.Builder
	b.WriteString("select *, count(*) over() as full_count from audit where 1=1 ")

	if startTime != "" && endTime != "" {
		b.WriteString("and stagetimestamp between to_timestamp(")
		b.WriteString(startTime)
		b.WriteString(") and to_timestamp(")
		b.WriteString(endTime)
		b.WriteString(") ")
	}

	if namespace != "" {
		b.WriteString("and namespace = '")
		b.WriteString(namespace)
		b.WriteString("' ")
	}

	if resource != "" {
		b.WriteString("and resource = '")
		b.WriteString(resource)
		b.WriteString("' ")
	}

	if status != "" {
		b.WriteString("and status = '")
		b.WriteString(status)
		b.WriteString("' ")
	}

	if verb != "" {
		b.WriteString("and verb = '")
		b.WriteString(verb)
		b.WriteString("' ")
	}

	if code != "" {
		codeNum, _ := strconv.ParseInt(code, 10, 32)
		lowerBound := (codeNum / 100) * 100
		upperBound := lowerBound + 99
		b.WriteString("and code between '")
		b.WriteString(fmt.Sprintf("%v", lowerBound))
		b.WriteString("' and '")
		b.WriteString(fmt.Sprintf("%v '", upperBound))
	}

	if sort != nil && len(sort) > 0 {
		b.WriteString("order by ")
		for _, s := range sort {
			order := " asc, "
			if string(s[0]) == "-" {
				order = " desc, "
				s = s[1:]
			}
			b.WriteString(s)
			b.WriteString(order)
		}
		b.WriteString("stagetimestamp desc ")
	} else {
		b.WriteString("order by stagetimestamp desc ")
	}

	if limit != "" {
		b.WriteString("limit ")
		b.WriteString(limit)
		b.WriteString(" ")
	} else {
		b.WriteString("limit 100 ")
	}

	if offset != "" {
		b.WriteString("offset ")
		b.WriteString(offset)
		b.WriteString(" ")
	} else {
		b.WriteString("offset 0 ")
	}
	query := b.String()

	klog.Info("query: ", query)
	return query
}
