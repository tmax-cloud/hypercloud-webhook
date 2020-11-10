package audit

import (
	"database/sql"
	"strings"

	_ "github.com/go-sql-driver/mysql"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apiserver/pkg/apis/audit"
	"k8s.io/klog"
)

const (
	AUDIT_INSERT_QUERY       = "insert into metering.audit values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"
	AUDIT_INSERT_QUERY_BATCH = "insert into metering.audit values"
	PARAMETER                = "(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"
	AUDIT_FOUND_ROWS_QUERY   = "SELECT FOUND_ROWS() as count"
)

func insert(items []audit.Event) {
	db, err := sql.Open("mysql", "root:tmax@tcp(mysql-service.hypercloud4-system.svc:3306)/metering?parseTime=true")
	if err != nil {
		klog.Error(err)
	}
	defer db.Close()

	var paramArray []string
	vals := []interface{}{}
	for _, event := range items {
		paramArray = append(paramArray, PARAMETER)
		vals = append(vals, event.AuditID,
			event.User.Username,
			event.UserAgent,
			event.ObjectRef.Namespace,
			event.ObjectRef.APIGroup,
			event.ObjectRef.APIVersion,
			event.ObjectRef.Resource,
			event.ObjectRef.Name,
			event.Stage,
			event.StageTimestamp.Time,
			event.Verb,
			event.ResponseStatus.Code,
			event.ResponseStatus.Status,
			event.ResponseStatus.Reason,
			event.ResponseStatus.Message)
	}
	query := AUDIT_INSERT_QUERY_BATCH + strings.Join(paramArray, ",")

	stmt, err := db.Prepare(query)
	if err != nil {
		klog.Info(err)
	}
	defer stmt.Close()

	res, err := stmt.Exec(vals...)
	if err != nil {
		klog.Info(err)
	}

	if count, err := res.RowsAffected(); err != nil {
		klog.Error(err)
	} else {
		klog.Info("Affected rows: ", count)
	}
}

func get(query string) (audit.EventList, int64) {
	db, err := sql.Open("mysql", "root:tmax@tcp(mysql-service.hypercloud4-system.svc:3306)/metering?parseTime=true")
	if err != nil {
		klog.Error(err)
	}
	defer db.Close()

	tx, err := db.Begin()
	if err != nil {
		klog.Error(err)
	}

	rows, err := tx.Query(query)
	eventList := audit.EventList{}
	for rows.Next() {
		event := audit.Event{
			ObjectRef:      &audit.ObjectReference{},
			ResponseStatus: &metav1.Status{},
		}
		err := rows.Scan(
			&event.AuditID,
			&event.User.Username,
			&event.UserAgent,
			&event.ObjectRef.Namespace,
			&event.ObjectRef.APIGroup,
			&event.ObjectRef.APIVersion,
			&event.ObjectRef.Resource,
			&event.ObjectRef.Name,
			&event.Stage,
			&event.StageTimestamp.Time,
			&event.Verb,
			&event.ResponseStatus.Code,
			&event.ResponseStatus.Status,
			&event.ResponseStatus.Reason,
			&event.ResponseStatus.Message)
		if err != nil {
			rows.Close()
			klog.Error(err)
		}
		event.StageTimestamp.Time = event.StageTimestamp.Time.Local()
		eventList.Items = append(eventList.Items, event)
	}
	eventList.Kind = "EventList"
	eventList.APIVersion = "audit.k8s.io/v1"

	if err != nil {
		tx.Rollback()
		rows.Close()
		klog.Error(err)
	}
	defer rows.Close()

	var count int64
	err = tx.QueryRow("SELECT FOUND_ROWS() as Count").Scan(&count)

	if err != nil {
		tx.Rollback()
		rows.Close()
		klog.Error(err)
	}
	return eventList, count
}

func getRowsCount() int64 {
	db, err := sql.Open("mysql", "root:tmax@tcp(mysql-service.hypercloud4-system.svc:3306)/metering?parseTime=true")
	if err != nil {
		klog.Error(err)
	}
	defer db.Close()

	var rowsCount int64
	err = db.QueryRow(AUDIT_FOUND_ROWS_QUERY).Scan(&rowsCount)
	if err != nil {
		klog.Error(err)
	}

	klog.Info("query: ", AUDIT_FOUND_ROWS_QUERY)
	klog.Infof("rowsCount %d", rowsCount)

	return rowsCount
}
