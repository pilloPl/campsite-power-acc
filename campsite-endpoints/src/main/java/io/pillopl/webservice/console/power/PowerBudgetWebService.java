package io.pillopl.webservice.console.power;

import io.pillopl.api.PowerBudgetApi;
import io.pillopl.commons.DateRange;
import io.pillopl.value.Username;
import io.pillopl.webservice.console.common.AbstractWebService;
import io.pillopl.webservice.console.common.WebServicePaths;
import io.pillopl.webservice.console.common.WsErrorCode;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Web service of the power budget tab: section switchboards, their load over time, capacity, allocation algorithm
 * and the sockets of a section.
 */
@RestController
@RequestMapping(WebServicePaths.POWER_BUDGET)
public class PowerBudgetWebService extends AbstractWebService<PowerBudgetApi, PowerBudgetWebServiceFault> {

    public PowerBudgetWebService(PowerBudgetApi powerBudgetApi) {
        super(powerBudgetApi);
    }

    @PostMapping("/findPowerSections")
    public FindPowerSectionsResponse findPowerSections(@RequestBody FindPowerSectionsRequest request)
            throws PowerBudgetWebServiceFault {
        return invoke("findPowerSections", () -> {
            Username caller = caller();
            return new FindPowerSectionsResponse(getBean().findPowerSections(caller),
                    getBean().findAllocationAlgorithms(caller));
        });
    }

    @PostMapping("/getSectionLoad")
    public GetSectionLoadResponse getSectionLoad(@RequestBody GetSectionLoadRequest request)
            throws PowerBudgetWebServiceFault {
        return invoke("getSectionLoad", () -> new GetSectionLoadResponse(getBean().getSectionLoad(caller(),
                request.sectionId(), DateRange.ofDays(request.from(), request.to()))));
    }

    @PostMapping("/savePowerSection")
    public PowerSectionResponse savePowerSection(@RequestBody SavePowerSectionRequest request)
            throws PowerBudgetWebServiceFault {
        return invoke("savePowerSection", () -> new PowerSectionResponse(getBean().savePowerSection(caller(),
                request.sectionId(), request.sectionName(), request.maxCurrentA(), request.algorithmBeanName())));
    }

    @PostMapping("/changeSectionCapacity")
    public PowerSectionResponse changeSectionCapacity(@RequestBody ChangeSectionCapacityRequest request)
            throws PowerBudgetWebServiceFault {
        return invoke("changeSectionCapacity", () -> new PowerSectionResponse(getBean().changeSectionCapacity(
                caller(), request.sectionId(), request.maxCurrentA())));
    }

    @PostMapping("/selectAllocationAlgorithm")
    public PowerSectionResponse selectAllocationAlgorithm(@RequestBody SelectAllocationAlgorithmRequest request)
            throws PowerBudgetWebServiceFault {
        return invoke("selectAllocationAlgorithm", () -> new PowerSectionResponse(getBean().selectAllocationAlgorithm(
                caller(), request.sectionId(), request.algorithmBeanName())));
    }

    @PostMapping("/assignSocketToSection")
    public PowerSectionResponse assignSocketToSection(@RequestBody AssignSocketToSectionRequest request)
            throws PowerBudgetWebServiceFault {
        return invoke("assignSocketToSection", () -> new PowerSectionResponse(getBean().assignSocketToSection(
                caller(), request.socketId(), request.sectionId())));
    }

    @Override
    protected PowerBudgetWebServiceFault createFault(String message, WsErrorCode errorCode) {
        return new PowerBudgetWebServiceFault(message, errorCode);
    }
}
