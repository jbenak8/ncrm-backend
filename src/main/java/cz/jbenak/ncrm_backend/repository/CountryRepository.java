package cz.jbenak.ncrm_backend.repository;

import cz.jbenak.ncrm_backend.model.entity.CountryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-11
 * Repository for countries (ISO code is the primary key).
 */
@Repository
public interface CountryRepository extends JpaRepository<CountryEntity, String> {

    List<CountryEntity> findAllByActiveTrue();
}
