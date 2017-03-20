/** Map data shaped like the demo backend answers it (one campsite, two pedestals, one section). */
export const mapData = {
  map: {
    campsites: [{
      groupId: 900000001, name: 'ISTRIA BAY CAMP', displayName: 'Camp Istria Bay', latitude: 45.15, longitude: 13.6,
      areas: [
        { id: 1, areaType: 'CAMPSITE', name: 'Camp Istria Bay',
          geoJson: '{"type":"Polygon","coordinates":[[[13.601,45.149],[13.604,45.149],[13.604,45.151],[13.601,45.151],[13.601,45.149]]]}' },
        { id: 2, areaType: 'SECTION', name: 'Section A', powerSectionId: 77,
          geoJson: '{"type":"Polygon","coordinates":[[[13.601,45.149],[13.602,45.149],[13.602,45.150],[13.601,45.150],[13.601,45.149]]]}' },
        { id: 3, areaType: 'PITCH', name: 'Pitch A01', pitchNo: 'A01', powerSectionId: 77, geoJson: 'not json' },
      ],
      pedestals: [
        { id: 101, code: 'ISTRIABAY-P01', latitude: 45.15, longitude: 13.6018, pedestalType: 'AQ-400', linkVersion: 'V2',
          remoteControl: true, timeout: false, lastIncoming: '2026-10-17T06:54:37',
          sockets: [
            { id: 11, socketNo: 1, name: 'ISTRIABAY-P01_1', pitchNo: 'A01', status: 'SUPPLYING', powerSectionId: 77, activeSessionId: 5001, sessionMaxCurrentA: 16 },
            { id: 12, socketNo: 2, name: 'ISTRIABAY-P01_2', pitchNo: 'A02', status: 'AVAILABLE', powerSectionId: 77 },
          ] },
        { id: 102, code: 'ISTRIABAY-P02', latitude: 45.1503, longitude: 13.6018, pedestalType: 'Campus 3', linkVersion: 'V3',
          remoteControl: true, timeout: false,
          sockets: [{ id: 24, socketNo: 4, name: 'ISTRIABAY-P02_4', pitchNo: 'A08', status: 'FAULTED', powerSectionId: 77 }] },
      ],
    }],
    sections: [{ id: 77, sectionName: 'ISTRIA BAY SECTION A', maxCurrentA: 80, sockets: 12, activeSessions: 6, allocatedCurrentA: 96, loadShare: 1.2 }],
  },
};
