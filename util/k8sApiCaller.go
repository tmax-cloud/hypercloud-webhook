package util

import (
	// "k8s.io/client-go/kubernetes"
	"context"
	"encoding/json"

	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	rest "k8s.io/client-go/rest"
	"k8s.io/klog"

	client "hypercloud4-webhook/client"

	v1alpha1 "github.com/tmax-cloud/efk-operator/api/v1alpha1"
)

var Clientset *client.Clientset

func init() {
	config, err := rest.InClusterConfig()
	if err != nil {
		panic(err.Error())
	}
	// creates the clientset

	Clientset, err = client.NewForConfig(config)
	if err != nil {
		panic(err.Error())
	}

}

func GetLogv1(namespace string, name string) (*v1alpha1.FluentBitConfiguration, error) {
	result, err := Clientset.ConfigV1alpha1().FluentBitConfigurations(namespace).Get(context.TODO(), name, metav1.GetOptions{})
	return result, err
}

func CreateLogv1() {
	var a []v1alpha1.InputPlugin
	a = append(a, v1alpha1.InputPlugin{
		Path:    "/test/log/sys_log",
		Pattern: "cho",
		Tag:     "cho",
	})

	var b []v1alpha1.FilterPlugin
	b = append(b, v1alpha1.FilterPlugin{
		ParserName: "cho",
		Regex:      "cho",
		Tag:        "cho",
	})

	var c []v1alpha1.OutputPlugin
	c = append(c, v1alpha1.OutputPlugin{
		IndexName: "cho",
		Tag:       "cho",
	})

	ls := v1alpha1.FluentBitConfigurationSpec{
		InputPlugins:  a,
		FilterPlugins: b,
		OutputPlugins: c,
	}
	test := v1alpha1.FluentBitConfiguration{
		TypeMeta: metav1.TypeMeta{
			Kind:       "FluentBitConfiguration",
			APIVersion: "config.tmax.io/v1alpha1",
		},
		ObjectMeta: metav1.ObjectMeta{
			Name:      "chov1",
			Namespace: "test",
		},
		Spec: ls,
	}

	// body, err := json.Marshal(test)
	// if err != nil {
	// 	klog.Errorln(err)
	// 	panic(err)
	// }
	// klog.Info(string(body))
	result, err := Clientset.ConfigV1alpha1().FluentBitConfigurations("test").Create(context.TODO(), &test, metav1.CreateOptions{})
	// ClusterRoleBindings().Create(context.TODO(), ClusterRoleBinding, metav1.CreateOptions{})
	if err != nil {
		klog.Errorln(err)
		panic(err)
	}

	klog.Info(result.GetName())
	// test :=  logv1.Log{}
	by, err := json.Marshal(result)
	klog.Info(string(by))

	by, err = json.Marshal(*result)
	klog.Info(string(by))

}
