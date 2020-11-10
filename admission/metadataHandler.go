package admission

import (
	"encoding/json"
	"time"

	util "hypercloud4-webhook/util"

	jsonpatch "github.com/evanphx/json-patch"
	"k8s.io/api/admission/v1beta1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/klog"
)

type Meta struct {
	metav1.TypeMeta   `json:",inline"`                                                 // kind & apigroup
	metav1.ObjectMeta `json:"metadata,omitempty" protobuf:"bytes,1,opt,name=metadata"` // annotation
}

func AddResourceMeta(ar v1beta1.AdmissionReview) *v1beta1.AdmissionResponse {
	reviewResponse := v1beta1.AdmissionResponse{}

	loc, _ := time.LoadLocation("Local")
	currentTime := time.Now().In(loc)

	userName := ar.Request.UserInfo.Username
	operation := string(ar.Request.Operation)
	ms := Meta{}
	oms := Meta{}
	diff := Meta{}

	if err := json.Unmarshal(ar.Request.Object.Raw, &ms); err != nil {
		return util.ToAdmissionResponse(err) //msg: error
	}

	if len(ar.Request.OldObject.Raw) > 0 {
		if err := json.Unmarshal(ar.Request.OldObject.Raw, &oms); err != nil {
			return util.ToAdmissionResponse(err) //msg: error
		}
		if mergePatch, err := jsonpatch.CreateMergePatch(ar.Request.OldObject.Raw, ar.Request.Object.Raw); err != nil {
			return util.ToAdmissionResponse(err) //msg: error
		} else {
			if err := json.Unmarshal(mergePatch, &diff); err != nil {
				return util.ToAdmissionResponse(err) //msg: error
			}
		}
	}

	if denyReq(ms, diff, operation) {
		return &v1beta1.AdmissionResponse{
			Result: &metav1.Status{
				Message: "Can not create/update resource metadata.",
			},
		}
	}

	var patch []util.PatchOps

	if len(ms.Annotations) == 0 {
		am := map[string]interface{}{
			"creator":     userName,
			"createdTime": currentTime,
			"updater":     userName,
			"updatedTime": currentTime,
		}
		util.CreatePatch(&patch, "add", "/metadata/annotations", am)
	} else {
		if _, ok := ms.Annotations["creator"]; !ok {
			util.CreatePatch(&patch, "add", "/metadata/annotations/creator", userName)
		}
		if _, ok := ms.Annotations["createdTime"]; !ok {
			util.CreatePatch(&patch, "add", "/metadata/annotations/createdTime", currentTime)
		}
		util.CreatePatch(&patch, "add", "/metadata/annotations/updater", userName)
		util.CreatePatch(&patch, "add", "/metadata/annotations/updatedTime", currentTime)
	}

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

func denyReq(ms, diff Meta, op string) bool {
	if op == "CREATE" {
		if _, ok := ms.Annotations["creator"]; ok {
			return true
		} else if _, ok := ms.Annotations["createdTime"]; ok {
			return true
		} else if _, ok := ms.Annotations["updater"]; ok {
			return true
		} else if _, ok := ms.Annotations["updatedTime"]; ok {
			return true
		}
	}

	if op == "UPDATE" {
		if _, ok := diff.Annotations["creator"]; ok {
			return true
		} else if _, ok := diff.Annotations["createdTime"]; ok {
			return true
		} else if _, ok := diff.Annotations["updater"]; ok {
			return true
		} else if _, ok := diff.Annotations["updatedTime"]; ok {
			return true
		}
	}

	return false
}
