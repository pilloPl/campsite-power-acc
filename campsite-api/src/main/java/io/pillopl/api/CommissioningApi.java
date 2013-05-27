package io.pillopl.api;

import io.pillopl.dto.infrastructure.CommissioningDTO;
import io.pillopl.dto.infrastructure.LocationDTO;
import io.pillopl.dto.infrastructure.PedestalImportDTO;
import io.pillopl.dto.infrastructure.SupplierStockEntryDTO;
import io.pillopl.value.InstallerToken;
import io.pillopl.value.PedestalCode;
import io.pillopl.value.PedestalId;
import io.pillopl.value.Username;

import java.util.List;

/**
 * Pedestal life cycle: supplier deliveries, the supplier stock, commissioning on an affiliate campsite and
 * decommissioning back to the supplier.
 */
public interface CommissioningApi {

    List<CommissioningDTO> findCommissionings(Username authUsername);

    CommissioningDTO findCommissioningByToken(Username installerUsername, InstallerToken token);

    CommissioningDTO createCommissioning(Username authUsername, Username ownerUsername, LocationDTO location,
                                         String comments, boolean listedInApp, String accessMode);

    List<SupplierStockEntryDTO> getSupplierStock(Username authUsername);

    /**
     * @return ids of the pedestals that could not be commissioned
     */
    List<Long> commissionPedestals(Username installerUsername, InstallerToken token, List<PedestalId> pedestalIds);

    void commissionPedestal(Username installerUsername, PedestalCode code, Username ownerUsername,
                            LocationDTO location, boolean listedInApp, String accessMode);

    void decommissionPedestal(Username authUsername, PedestalId pedestalId);

    /**
     * @return codes of the imported pedestals, in the order of the delivery
     */
    List<String> importSupplierPedestals(Username authUsername, List<PedestalImportDTO> pedestals,
                                         boolean networkOwned);
}
