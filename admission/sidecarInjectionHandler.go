package admission

import (
	"encoding/json"
	"errors"
	util "hypercloud4-webhook/util"
	"strings"

	"k8s.io/api/admission/v1beta1"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/klog"

	authv1 "k8s.io/api/authentication/v1"
)

var SidecarContainerImage string

func SidecarInjection(ar v1beta1.AdmissionReview) *v1beta1.AdmissionResponse {
	reviewResponse := v1beta1.AdmissionResponse{}
	pod := corev1.Pod{}

	if err := json.Unmarshal(ar.Request.Object.Raw, &pod); err != nil {
		return util.ToAdmissionResponse(err)
	}

	klog.Info(string(ar.Request.Object.Raw))

	if isSystemRequest(ar.Request.UserInfo) {
		return util.ToAdmissionResponse(nil)
	}

	var configName string
	if val, exist := pod.Annotations["tmax.io/log-collector-configuration"]; exist {
		configName = val
	} else {
		err := errors.New("Log collector configuration is empty.")
		klog.Error(err)
		return util.ToAdmissionResponse(errors.New("Log collector configuration is empty."))
	}
	var logRootPath string
	if val, exist := pod.Annotations["tmax.io/log-root-path"]; exist {
		logRootPath = val
	} else {
		err := errors.New("Log root path is empty.")
		klog.Error(err)
		return util.ToAdmissionResponse(errors.New("Log root path is empty."))
	}

	volumeMounts := []corev1.VolumeMount{}
	// Build volumeMount for sidecar container
	volumeMounts = append(volumeMounts, corev1.VolumeMount{
		Name:      "shared",
		MountPath: "/shared" + logRootPath,
	})

	// TO DO
	volumeMounts = append(volumeMounts, corev1.VolumeMount{
		Name:      "fluent-bit-config",
		MountPath: "/fluent-bit/etc/fluent-bit.conf",
		SubPath:   "fluent-bit.conf",
	})

	volumeMounts = append(volumeMounts, corev1.VolumeMount{
		Name:      "fluent-bit-config",
		MountPath: "/fluent-bit/etc/user-parser.conf",
		SubPath:   "user-parser.conf",
	})

	oldContainerList := pod.Spec.Containers
	newContainerList := []corev1.Container{}

	for _, container := range oldContainerList {
		container.VolumeMounts = append(container.VolumeMounts, corev1.VolumeMount{
			Name:      "shared",
			MountPath: "/shared" + logRootPath,
			ReadOnly:  true,
		})
		newContainerList = append(newContainerList, container)
	}

	containerPatch := append(newContainerList, corev1.Container{
		Name:         "fluent-bit",
		Image:        SidecarContainerImage,
		VolumeMounts: volumeMounts,
	})

	sharedVolumePatch := corev1.Volume{
		Name: "shared",
		VolumeSource: corev1.VolumeSource{
			EmptyDir: &corev1.EmptyDirVolumeSource{},
		},
	}

	// Patch for pod volumes... (configmap volume)
	configmapVolumePatch := corev1.Volume{
		Name: "fluent-bit-config",
		VolumeSource: corev1.VolumeSource{
			ConfigMap: &corev1.ConfigMapVolumeSource{
				LocalObjectReference: corev1.LocalObjectReference{
					Name: configName,
				},
			},
		},
	}

	var patch []util.PatchOps
	util.CreatePatch(&patch, "add", "/spec/containers", containerPatch)
	util.CreatePatch(&patch, "add", "/spec/volumes/-", sharedVolumePatch)
	util.CreatePatch(&patch, "add", "/spec/volumes/-", configmapVolumePatch)

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

func isSystemRequest(userInfo authv1.UserInfo) bool {
	//group에 대해서 먼저 ...
	for _, group := range userInfo.Groups {
		gorupElement := strings.Split(group, ":")
		if gorupElement[0] == "system" && ((gorupElement[1] != "masters") && (gorupElement[1] != "authenticated")) { //for kubectl
			// reject
			return true
		}
	}
	// 그다음 user..?
	userNameElement := strings.Split(userInfo.Username, ":")
	if userNameElement[0] == "system" {
		// reject
		return true
	}
	return false
}
