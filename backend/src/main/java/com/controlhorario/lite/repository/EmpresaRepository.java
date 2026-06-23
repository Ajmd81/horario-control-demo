package com.controlhorario.lite.repository;

import com.controlhorario.lite.entity.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmpresaRepository extends JpaRepository<Empresa, Long> {

    Optional<Empresa> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Optional<Empresa> findByStripeCustomerId(String stripeCustomerId);

    // Necesario para StripeSyncService
    List<Empresa> findAllByStripeSubscriptionIdIsNotNull();
}