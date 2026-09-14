package org.nagrivic.modules.push.repository;

import org.nagrivic.modules.push.entity.PushDeliveryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PushDeliveryRepository extends JpaRepository<PushDeliveryEntity, UUID> {

    List<PushDeliveryEntity> findByNotification_Id(UUID notificationId);

    List<PushDeliveryEntity> findByDevice_Id(UUID deviceId);
}
