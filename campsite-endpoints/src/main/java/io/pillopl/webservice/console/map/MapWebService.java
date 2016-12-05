package io.pillopl.webservice.console.map;

import io.pillopl.api.MapApi;
import io.pillopl.webservice.console.common.AbstractWebService;
import io.pillopl.webservice.console.common.WebServicePaths;
import io.pillopl.webservice.console.common.WsErrorCode;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Web service of the map tab; the popup's remote supply start and stop are calls of the SupplyWebService.
 */
@RestController
@RequestMapping(WebServicePaths.MAP)
public class MapWebService extends AbstractWebService<MapApi, MapWebServiceFault> {

    public MapWebService(MapApi mapApi) {
        super(mapApi);
    }

    @PostMapping("/getMapData")
    public GetMapDataResponse getMapData(@RequestBody GetMapDataRequest request) throws MapWebServiceFault {
        return invoke("getMapData", () -> new GetMapDataResponse(getBean().getMapData(caller(),
                request.campsiteGroupId())));
    }

    @Override
    protected MapWebServiceFault createFault(String message, WsErrorCode errorCode) {
        return new MapWebServiceFault(message, errorCode);
    }
}
