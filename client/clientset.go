package client

import (
	"fmt"
	configv1alpha1 "hypercloud4-webhook/client/typed/config/v1alpha1"

	// v1alpha1 "github.com/tmax-cloud/efk-operator/api/v1alpha1"

	discovery "k8s.io/client-go/discovery"
	"k8s.io/client-go/rest"
	"k8s.io/client-go/util/flowcontrol"
)

type Interface interface {
	Discovery() discovery.DiscoveryInterface
	ConfigV1alpha1() configv1alpha1.ConfigV1alpha1Interface
}

type Clientset struct {
	*discovery.DiscoveryClient
	configV1alpha1 *configv1alpha1.ConfigV1alpha1Client
}

// CoreV1 retrieves the CoreV1Client
func (c *Clientset) ConfigV1alpha1() configv1alpha1.ConfigV1alpha1Interface {
	return c.configV1alpha1
}

func NewForConfig(c *rest.Config) (*Clientset, error) {
	configShallowCopy := *c
	if configShallowCopy.RateLimiter == nil && configShallowCopy.QPS > 0 {
		if configShallowCopy.Burst <= 0 {
			return nil, fmt.Errorf("burst is required to be greater than 0 when RateLimiter is not set and QPS is set to greater than 0")
		}
		configShallowCopy.RateLimiter = flowcontrol.NewTokenBucketRateLimiter(configShallowCopy.QPS, configShallowCopy.Burst)
	}
	var cs Clientset
	var err error

	cs.configV1alpha1, err = configv1alpha1.NewForConfig(&configShallowCopy)
	if err != nil {
		return nil, err
	}
	return &cs, nil
}
