package cz.jbenak.ncrm_backend.model.mapper;

import cz.jbenak.ncrm_backend.configuration.MapstructConfig;
import cz.jbenak.ncrm_backend.model.dto.store.ItemCategoryDto;
import cz.jbenak.ncrm_backend.model.dto.store.ItemDto;
import cz.jbenak.ncrm_backend.model.dto.store.ItemPriceDto;
import cz.jbenak.ncrm_backend.model.entity.store.ItemCategoryEntity;
import cz.jbenak.ncrm_backend.model.entity.store.ItemEntity;
import cz.jbenak.ncrm_backend.model.entity.store.ItemPriceEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-11
 * MapStruct mapper for items, item prices and the item category tree.
 */
@Mapper(config = MapstructConfig.class)
public interface ItemMapper {

    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "hasImage", expression = "java(entity.getImage() != null)")
    ItemDto toDto(ItemEntity entity);

    List<ItemDto> toDtoList(List<ItemEntity> entities);

    ItemPriceDto toDto(ItemPriceEntity entity);

    @Mapping(target = "parentId", source = "parent.id")
    ItemCategoryDto toDto(ItemCategoryEntity entity);

    List<ItemCategoryDto> toCategoryDtoList(List<ItemCategoryEntity> entities);
}
