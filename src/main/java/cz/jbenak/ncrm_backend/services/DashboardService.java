package cz.jbenak.ncrm_backend.services;

import cz.jbenak.ncrm_backend.model.dto.dashboard.DashboardDtos;
import cz.jbenak.ncrm_backend.model.entity.customer.MeetingEntity;
import cz.jbenak.ncrm_backend.model.entity.marketing.CampaignEntity;
import cz.jbenak.ncrm_backend.model.entity.order.OrderEntity;
import cz.jbenak.ncrm_backend.repository.CampaignRepository;
import cz.jbenak.ncrm_backend.repository.CustomerRepository;
import cz.jbenak.ncrm_backend.repository.MeetingRepository;
import cz.jbenak.ncrm_backend.repository.OrderRepository;
import cz.jbenak.ncrm_backend.repository.SalesRepresentativeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-11
 * Service aggregating business data for the owner dashboards displayed in the React frontend
 * (summary KPIs, revenue by month, performance of sales representatives, top customers).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final MeetingRepository meetingRepository;
    private final CampaignRepository campaignRepository;
    private final SalesRepresentativeRepository salesRepresentativeRepository;

    public DashboardDtos.OwnerDashboard ownerDashboard() {
        log.debug("Building the owner dashboard payload");
        return new DashboardDtos.OwnerDashboard(summary(), ordersByMonth(), salesByRepresentative(), topCustomers(10),
                activeCampaigns());
    }

    public DashboardDtos.DashboardSummary summary() {
        return new DashboardDtos.DashboardSummary(
                customerRepository.count(),
                customerRepository.countByActiveTrue(),
                orderRepository.count(),
                orderRepository.countByStatusIn(List.of(
                        OrderEntity.OrderStatus.NEW, OrderEntity.OrderStatus.CONFIRMED, OrderEntity.OrderStatus.IN_PROGRESS)),
                orderRepository.sumTotalRevenue(),
                meetingRepository.countByStatus(MeetingEntity.MeetingStatus.PLANNED),
                meetingRepository.countByStatus(MeetingEntity.MeetingStatus.COMPLETED),
                campaignRepository.countByStatus(CampaignEntity.CampaignStatus.SENT));
    }

    public List<DashboardDtos.OrdersByMonth> ordersByMonth() {
        return orderRepository.aggregateOrdersByMonth().stream()
                .map(row -> new DashboardDtos.OrdersByMonth((String) row[0], (Long) row[1], (BigDecimal) row[2]))
                .toList();
    }

    public List<DashboardDtos.SalesByRepresentative> salesByRepresentative() {
        return orderRepository.aggregateOrdersBySalesRepresentative().stream()
                .map(row -> {
                    UUID repId = (UUID) row[0];
                    String name = salesRepresentativeRepository.findById(repId)
                            .map(rep -> rep.getUser().getFirstName() + " " + rep.getUser().getLastName())
                            .orElse("Unknown");
                    return new DashboardDtos.SalesByRepresentative(repId, name, (Long) row[1], (BigDecimal) row[2],
                            meetingRepository.countBySalesRepresentativeId(repId));
                })
                .toList();
    }

    /**
     * Campaigns that are being prepared or are waiting to be sent (draft, scheduled or sending).
     */
    public List<DashboardDtos.ActiveCampaign> activeCampaigns() {
        return campaignRepository.findAllByStatusInOrderByScheduledAtAscNameAsc(List.of(
                        CampaignEntity.CampaignStatus.DRAFT,
                        CampaignEntity.CampaignStatus.SCHEDULED,
                        CampaignEntity.CampaignStatus.SENDING)).stream()
                .map(c -> new DashboardDtos.ActiveCampaign(c.getId(), c.getName(), c.getSubject(),
                        c.getStatus().name(), c.getScheduledAt(), c.getRecipients().size()))
                .toList();
    }

    public List<DashboardDtos.TopCustomer> topCustomers(int limit) {
        return orderRepository.aggregateTopCustomers(limit).stream()
                .map(row -> new DashboardDtos.TopCustomer((UUID) row[0], (String) row[1], (Long) row[2], (BigDecimal) row[3]))
                .toList();
    }
}
