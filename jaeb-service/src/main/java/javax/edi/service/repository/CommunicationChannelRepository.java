package javax.edi.service.repository;

import java.util.List;

import javax.edi.service.entity.CommunicationChannel;
import javax.edi.service.entity.CommunicationChannel.ChannelStatus;
import javax.edi.service.entity.CommunicationChannel.Direction;
import javax.edi.service.entity.CommunicationChannel.Protocol;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommunicationChannelRepository extends JpaRepository<CommunicationChannel, Long> {

    List<CommunicationChannel> findByPartnerId(String partnerId);

    List<CommunicationChannel> findByTenantId(String tenantId);

    List<CommunicationChannel> findByPartnerIdAndDirection(String partnerId, Direction direction);

    List<CommunicationChannel> findByPartnerIdAndProtocol(String partnerId, Protocol protocol);

    List<CommunicationChannel> findByStatusAndDirectionAndProtocolIn(
            ChannelStatus status, Direction direction, List<Protocol> protocols);

    /** All active inbound channels (for scheduled polling). */
    List<CommunicationChannel> findByStatusAndDirection(ChannelStatus status, Direction direction);
}
