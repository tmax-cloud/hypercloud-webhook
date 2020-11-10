package admission

import (
	"encoding/json"
	"errors"
	util "hypercloud4-webhook/util"

	"k8s.io/api/admission/v1beta1"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/klog"
)

var SidecarContainerImage string

func SidecarInjection(ar v1beta1.AdmissionReview) *v1beta1.AdmissionResponse {
	reviewResponse := v1beta1.AdmissionResponse{}
	pod := corev1.Pod{}

	if err := json.Unmarshal(ar.Request.Object.Raw, &pod); err != nil {
		return util.ToAdmissionResponse(err)
	}

	klog.Info(string(ar.Request.Object.Raw))

	var volumeNmae string
	configMap := corev1.ConfigMap{}
	if val, exist := pod.Annotations["tmax.io/sharedVolumeName"]; exist {
		volumeNmae = val
		klog.Info("volumeNmae", volumeNmae)
	} else {
		err := errors.New("Shared volume name is required.")
		klog.Error(err)
		return util.ToAdmissionResponse(err)
	}
	if val, exist := pod.Annotations["tmax.io/configMap"]; exist {
		json.Unmarshal([]byte(val), &configMap)
	} else {
		err := errors.New("Raw configmap data is required.")
		klog.Error(err)
		return util.ToAdmissionResponse(errors.New("Raw configmap data is required."))
	}

	containerList := pod.Spec.Containers
	volumeMounts := []corev1.VolumeMount{}

	// Build volumeMount for sidecar container
	volumeMounts = append(volumeMounts, corev1.VolumeMount{
		Name:      volumeNmae,
		MountPath: "/shared",
	})

	// TO DO
	for key, _ := range configMap.Data {
		volumeMounts = append(volumeMounts, corev1.VolumeMount{
			Name:      "fluent-bit-config",
			MountPath: "/fluent-bit/etc/" + key,
			SubPath:   key,
		})
	}
	// Patch for container list with above volumeMounts
	containerPatch := append(containerList, corev1.Container{
		Name:         "fluent-bit",
		Image:        SidecarContainerImage,
		VolumeMounts: volumeMounts,
	})

	// Patch for pod volumes... (configmap volume)
	volumePatch := corev1.Volume{
		Name: "fluent-bit-config",
		VolumeSource: corev1.VolumeSource{
			ConfigMap: &corev1.ConfigMapVolumeSource{
				LocalObjectReference: corev1.LocalObjectReference{
					Name: configMap.Name,
				},
			},
		},
	}

	var patch []util.PatchOps
	util.CreatePatch(&patch, "add", "/spec/containers", containerPatch)
	util.CreatePatch(&patch, "add", "/spec/volumes/-", volumePatch)

	if patchData, err := json.Marshal(patch); err != nil {
		return util.ToAdmissionResponse(err) //msg: error
	} else {
		klog.Infof("JsonPatch=%s", string(patchData))
		reviewResponse.Patch = patchData
	}

	pt := v1beta1.PatchTypeJSONPatch
	reviewResponse.PatchType = &pt
	reviewResponse.Allowed = true

	return &reviewResponse
}
