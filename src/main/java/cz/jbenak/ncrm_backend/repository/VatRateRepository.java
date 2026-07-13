package cz.jbenak.ncrm_backend.repository;

import cz.jbenak.ncrm_backend.model.entity.VATRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-13
 * Repository for VAT rates of countries.
 */
@Repository
public interface VatRateRepository extends JpaRepository<VATRate, UUID> {

    List<VATRate> findAllByCountryIsoCodeOrderByValidFromDesc(String countryIsoCode);
}
