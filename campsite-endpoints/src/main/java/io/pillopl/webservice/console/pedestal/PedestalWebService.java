package io.pillopl.webservice.console.pedestal;

import io.pillopl.api.PedestalApi;
import io.pillopl.commons.ListPage;
import io.pillopl.commons.Pagination;
import io.pillopl.dto.infrastructure.PedestalSummaryDTO;
import io.pillopl.value.PedestalCode;
import io.pillopl.value.PitchNo;
import io.pillopl.value.SocketCode;
import io.pillopl.value.SocketId;
import io.pillopl.value.Username;
import io.pillopl.webservice.console.common.AbstractWebService;
import io.pillopl.webservice.console.common.WebServicePaths;
import io.pillopl.webservice.console.common.WsErrorCode;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Web service of the pedestals tab: list, detail, address, pitches and free sockets.
 */
@RestController
@RequestMapping(WebServicePaths.PEDESTAL)
public class PedestalWebService extends AbstractWebService<PedestalApi, PedestalWebServiceFault> {

    public PedestalWebService(PedestalApi pedestalApi) {
        super(pedestalApi);
    }

    @PostMapping("/findPedestalsByState")
    public FindPedestalsByStateResponse findPedestalsByState(@RequestBody FindPedestalsByStateRequest request)
            throws PedestalWebServiceFault {
        return invoke("findPedestalsByState", () -> {
            ListPage<PedestalSummaryDTO> page = getBean().findPedestalsByState(caller(),
                    request.state(), Pagination.ofNullable(request.page(), request.rowsPerPage()));
            return new FindPedestalsByStateResponse(page.items(), page.total(), page.page(), page.rowsPerPage());
        });
    }

    @PostMapping("/getPedestal")
    public GetPedestalResponse getPedestal(@RequestBody GetPedestalRequest request) throws PedestalWebServiceFault {
        return invoke("getPedestal", () -> new GetPedestalResponse(getBean().getPedestal(
                caller(), PedestalCode.of(request.pedestalCode()))));
    }

    @PostMapping("/findPedestalBySocketCode")
    public FindPedestalBySocketCodeResponse findPedestalBySocketCode(
            @RequestBody FindPedestalBySocketCodeRequest request) throws PedestalWebServiceFault {
        return invoke("findPedestalBySocketCode", () -> new FindPedestalBySocketCodeResponse(
                getBean().findPedestalBySocketCode(caller(),
                        SocketCode.parse(request.socketCode()))));
    }

    @PostMapping("/deletePedestal")
    public DeletePedestalResponse deletePedestal(@RequestBody DeletePedestalRequest request)
            throws PedestalWebServiceFault {
        return invoke("deletePedestal", () -> {
            PedestalCode code = PedestalCode.of(request.pedestalCode());
            getBean().deletePedestal(caller(), Username.of(request.ownerUsername()), code);
            return new DeletePedestalResponse(code.value(), true);
        });
    }

    @PostMapping("/updateLocation")
    public UpdateLocationResponse updateLocation(@RequestBody UpdateLocationRequest request)
            throws PedestalWebServiceFault {
        return invoke("updateLocation", () -> new UpdateLocationResponse(getBean().updateLocation(
                caller(), Username.of(request.ownerUsername()),
                PedestalCode.of(request.pedestalCode()), request.location())));
    }

    @PostMapping("/assignPitch")
    public AssignPitchResponse assignPitch(@RequestBody AssignPitchRequest request) throws PedestalWebServiceFault {
        return invoke("assignPitch", () -> new AssignPitchResponse(getBean().assignPitch(
                caller(), SocketId.of(request.socketId()),
                PitchNo.ofNullable(request.pitchNo()))));
    }

    @PostMapping("/setFreeSocket")
    public SetFreeSocketResponse setFreeSocket(@RequestBody SetFreeSocketRequest request)
            throws PedestalWebServiceFault {
        return invoke("setFreeSocket", () -> new SetFreeSocketResponse(getBean().setFreeSocket(
                caller(), SocketId.of(request.socketId()),
                Boolean.TRUE.equals(request.freeSocket()))));
    }

    @Override
    protected PedestalWebServiceFault createFault(String message, WsErrorCode errorCode) {
        return new PedestalWebServiceFault(message, errorCode);
    }
}
