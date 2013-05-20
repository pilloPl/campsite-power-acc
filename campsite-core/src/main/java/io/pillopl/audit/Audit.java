package io.pillopl.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * One audited action. Free text columns are abbreviated to their length by {@link AuditService#insert}.
 */
@Entity
@Table(name = "AUDIT")
@Getter
@Setter
public class Audit {

    public static final String SUCCESS = "SUCCESS";

    public static final String ERROR = "ERROR";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_audit")
    @SequenceGenerator(name = "seq_audit", sequenceName = "AUDIT_ID_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "DATE_CREATED", nullable = false)
    private LocalDateTime dateCreated = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "CATEGORY", length = 10, nullable = false)
    private AuditCategory category;

    @Column(name = "USERID", length = 100)
    private String userId;

    @Column(name = "ACTION_DATE")
    private LocalDateTime actionDate;

    @Column(name = "ACTION_TYPE", length = 50)
    private String actionType;

    @Column(name = "ACTION_NAME", length = 100)
    private String actionName;

    @Column(name = "ACTION_GROUP_ID", length = 50)
    private String actionGroupId;

    @Column(name = "STATUS", length = 15)
    private String status;

    @Column(name = "ERROR_REASON", length = 2000)
    private String errorReason;

    @Column(name = "MANAGED_SERVER", length = 50)
    private String managedServer;

    @Column(name = "INFO_FIELD_1", length = 200)
    private String infoField1;

    @Column(name = "INFO_FIELD_2", length = 200)
    private String infoField2;

    @Column(name = "MESSAGE", length = 2000)
    private String message;

    @Column(name = "AUDIT_DATA", length = 2000)
    private String auditData;

    @Override
    public String toString() {
        return "Audit{id=" + id + ", " + category + ", " + actionType + ", " + actionName + ", " + status + "}";
    }
}
