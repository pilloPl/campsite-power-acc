package io.pillopl.pricing;

import io.pillopl.dto.pricing.PriceComponentDTO;
import io.pillopl.dto.pricing.PriceSimulationDTO;
import io.pillopl.dto.pricing.PriceSimulationLineDTO;
import io.pillopl.dto.pricing.TariffApplicabilityDTO;
import io.pillopl.dto.pricing.TariffDTO;
import io.pillopl.dto.pricing.TariffElementDTO;
import io.pillopl.dto.pricing.TariffRestrictionDTO;
import io.pillopl.group.Market;
import io.pillopl.util.ApiValues;
import io.pillopl.value.Money;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;

/**
 * Tariff entities to transfer objects and back (a tariff sent by the console becomes a new, unsaved entity).
 */
public final class TariffMapper {

    private TariffMapper() {
        throw new AssertionError();
    }

    public static TariffDTO toDTO(Tariff tariff) {
        if (tariff == null) {
            return null;
        }
        List<TariffElementDTO> elements = new ArrayList<>();
        for (TariffElement element : tariff.getTariffElements()) {
            elements.add(toDTO(element));
        }
        return new TariffDTO(tariff.getId(), tariff.getName(), tariff.getDescription(),
                tariff.getMarket() != null ? tariff.getMarket().name() : null, tariff.getCurrency(),
                tariff.isDefaultTariff(), tariff.getLastUpdateTime(), TariffPriceText.describe(tariff), elements);
    }

    public static List<TariffDTO> toDTOs(List<Tariff> tariffs) {
        List<TariffDTO> result = new ArrayList<>(tariffs.size());
        for (Tariff tariff : tariffs) {
            result.add(toDTO(tariff));
        }
        return result;
    }

    static TariffElementDTO toDTO(TariffElement element) {
        List<PriceComponentDTO> components = new ArrayList<>();
        for (PriceComponent component : element.getPriceComponents()) {
            components.add(new PriceComponentDTO(component.getId(), component.getDimensionType().name(),
                    component.getPrice(), component.getStepSize(), component.getPriceUnit().name()));
        }
        return new TariffElementDTO(element.getId(), element.getPosition(), components,
                toDTO(element.getTariffRestriction()));
    }

    static TariffRestrictionDTO toDTO(TariffRestriction restriction) {
        if (restriction == null) {
            return null;
        }
        List<String> days = new ArrayList<>();
        restriction.getDaysOfWeek().stream().sorted().forEach(day -> days.add(day.name()));
        return new TariffRestrictionDTO(restriction.getStartTime(), restriction.getEndTime(),
                restriction.getStartDate(), restriction.getEndDate(), days, restriction.getMinKwh(),
                restriction.getMaxKwh());
    }

    /**
     * @return a new tariff entity (no id); the market is set by the service from the calling user
     */
    public static Tariff toEntity(TariffDTO dto) {
        Tariff tariff = new Tariff(dto.name() != null ? dto.name().trim() : null, dto.description(), null);
        if (dto.market() != null && !dto.market().isBlank()) {
            tariff.setMarket(ApiValues.parseEnum(Market.class, dto.market(), "market"));
        }
        if (dto.currency() != null && !dto.currency().isBlank()) {
            tariff.setCurrency(dto.currency().trim());
        }
        tariff.setDefaultTariff(dto.defaultTariff());
        if (dto.elements() != null) {
            for (TariffElementDTO elementDTO : dto.elements()) {
                tariff.addElement(toEntity(elementDTO));
            }
        }
        return tariff;
    }

    static TariffElement toEntity(TariffElementDTO dto) {
        TariffElement element = new TariffElement();
        if (dto.priceComponents() != null) {
            for (PriceComponentDTO componentDTO : dto.priceComponents()) {
                element.addComponent(new PriceComponent(
                        ApiValues.parseEnum(TariffDimensionType.class, componentDTO.dimensionType(), "dimension type"),
                        componentDTO.price(), componentDTO.stepSize() != null ? componentDTO.stepSize() : 1));
            }
        }
        TariffRestrictionDTO restrictionDTO = dto.restriction();
        if (restrictionDTO != null) {
            TariffRestriction restriction = new TariffRestriction();
            restriction.setStartTime(restrictionDTO.startTime());
            restriction.setEndTime(restrictionDTO.endTime());
            restriction.setStartDate(restrictionDTO.startDate());
            restriction.setEndDate(restrictionDTO.endDate());
            restriction.setMinKwh(restrictionDTO.minKwh());
            restriction.setMaxKwh(restrictionDTO.maxKwh());
            if (restrictionDTO.daysOfWeek() != null) {
                for (String day : restrictionDTO.daysOfWeek()) {
                    restriction.getDaysOfWeek().add(ApiValues.parseEnum(DayOfWeek.class, day, "day of week"));
                }
            }
            element.restrictTo(restriction);
        }
        return element;
    }

    public static TariffApplicabilityDTO toDTO(TariffApplicability applicability) {
        if (applicability == null) {
            return null;
        }
        Long pedestalId = applicability.getPedestal() != null ? applicability.getPedestal().getId()
                : applicability.getSocket().getPedestal().getId();
        String pedestalCode = applicability.getPedestal() != null ? applicability.getPedestal().getCode()
                : applicability.getSocket().getPedestal().getCode();
        return new TariffApplicabilityDTO(applicability.getId(), applicability.getTariff().getId(),
                applicability.getTariff().getName(),
                applicability.getSocket() != null ? applicability.getSocket().getId() : null, pedestalId, pedestalCode,
                applicability.getStartDate(), applicability.getCreatedBy());
    }

    public static List<TariffApplicabilityDTO> toApplicabilityDTOs(List<TariffApplicability> applicabilities) {
        List<TariffApplicabilityDTO> result = new ArrayList<>(applicabilities.size());
        for (TariffApplicability applicability : applicabilities) {
            result.add(toDTO(applicability));
        }
        return result;
    }

    public static PriceSimulationDTO toDTO(PriceCalculation calculation) {
        List<PriceSimulationLineDTO> lines = new ArrayList<>();
        for (PriceCalculationLine line : calculation.getLines()) {
            lines.add(new PriceSimulationLineDTO(line.elementPosition(), line.dimensionType().name(), line.price(),
                    line.priceUnit().name(), line.share(), line.billedQuantity(), line.displayAmount()));
        }
        Money cost = calculation.getCurrency() != null
                ? Money.ofNullable(calculation.getCost(), calculation.getCurrency()) : null;
        return new PriceSimulationDTO(calculation.getStatus().name(), calculation.getCost(),
                calculation.getCurrency(), cost != null ? cost.toString() : null, calculation.getTariffId(),
                calculation.getTariffName(),
                calculation.getTariffSource() != null ? calculation.getTariffSource().name() : null,
                calculation.getFailureReason(), lines);
    }
}
