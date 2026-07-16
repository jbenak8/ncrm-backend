package cz.jbenak.ncrm_backend.model.mapper;

import cz.jbenak.ncrm_backend.configuration.MapstructConfig;
import cz.jbenak.ncrm_backend.model.dto.invoice.InvoiceDto;
import cz.jbenak.ncrm_backend.model.dto.invoice.InvoiceItemDto;
import cz.jbenak.ncrm_backend.model.entity.invoice.InvoiceEntity;
import cz.jbenak.ncrm_backend.model.entity.invoice.InvoiceItemEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-16
 * MapStruct mapper for issued invoices and their lines. Entities are created by the service
 * because the invoice is an immutable snapshot of the order computed server-side.
 */
@Mapper(config = MapstructConfig.class)
public interface InvoiceMapper {

    @Mapping(target = "orderId", source = "order.id")
    @Mapping(target = "orderNumber", source = "order.orderNumber")
    @Mapping(target = "customerId", source = "order.customer.id")
    @Mapping(target = "customerName", source = "order.customer.name")
    InvoiceDto toDto(InvoiceEntity entity);

    List<InvoiceDto> toDtoList(List<InvoiceEntity> entities);

    InvoiceItemDto toDto(InvoiceItemEntity entity);
}
