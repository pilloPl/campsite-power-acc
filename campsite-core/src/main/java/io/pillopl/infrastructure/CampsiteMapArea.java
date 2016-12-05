package io.pillopl.infrastructure;

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

/**
 * Outline of a campsite, a section or a pitch on the console map as a GeoJSON geometry (CAMPSITE_MAP_AREA).
 */
@Entity
@Table(name = "CAMPSITE_MAP_AREA")
@Getter
@Setter
public class CampsiteMapArea {

    public enum AreaType {
        CAMPSITE,
        SECTION,
        PITCH
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_campsite_map_area")
    @SequenceGenerator(name = "seq_campsite_map_area", sequenceName = "CAMPSITE_MAP_AREA_ID_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "SITE_GROUP_ID", nullable = false)
    private Long siteGroupId;

    @Enumerated(EnumType.STRING)
    @Column(name = "AREA_TYPE", length = 16, nullable = false)
    private AreaType areaType;

    @Column(name = "NAME", length = 128, nullable = false)
    private String name;

    @Column(name = "PITCH_NO", length = 16)
    private String pitchNo;

    @Column(name = "POWER_SECTION_ID")
    private Long powerSectionId;

    @Column(name = "GEOJSON", nullable = false, columnDefinition = "TEXT")
    private String geoJson;

    public CampsiteMapArea() {
    }

    public CampsiteMapArea(Long siteGroupId, AreaType areaType, String name, String geoJson) {
        this.siteGroupId = siteGroupId;
        this.areaType = areaType;
        this.name = name;
        this.geoJson = geoJson;
        if (areaType == AreaType.PITCH) {
            this.pitchNo = name;
        }
    }

    @Override
    public String toString() {
        return "CampsiteMapArea{" + areaType + " " + name + ", group=" + siteGroupId + "}";
    }
}
