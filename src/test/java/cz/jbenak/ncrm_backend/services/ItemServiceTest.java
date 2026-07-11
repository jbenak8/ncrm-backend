package cz.jbenak.ncrm_backend.services;

import cz.jbenak.ncrm_backend.model.dto.store.ItemDto;
import cz.jbenak.ncrm_backend.model.entity.store.ItemEntity;
import cz.jbenak.ncrm_backend.model.mapper.ItemMapper;
import cz.jbenak.ncrm_backend.repository.ItemCategoryRepository;
import cz.jbenak.ncrm_backend.repository.ItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests of {@link ItemService} covering the read-only store catalogue operations.
 */
@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;
    @Mock
    private ItemCategoryRepository itemCategoryRepository;
    @Mock
    private ItemMapper itemMapper;

    @InjectMocks
    private ItemService itemService;

    @Test
    void findAllActiveDelegatesToRepository() {
        when(itemRepository.findAllByActiveTrue()).thenReturn(List.of());

        itemService.findAllActive();

        verify(itemMapper).toDtoList(List.of());
    }

    @Test
    void findByIdMapsExistingItem() {
        UUID id = UUID.randomUUID();
        ItemEntity item = new ItemEntity();
        when(itemRepository.findById(id)).thenReturn(Optional.of(item));
        when(itemMapper.toDto(item)).thenReturn(mock(ItemDto.class));

        itemService.findById(id);

        verify(itemMapper).toDto(item);
    }

    @Test
    void findByIdThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(itemRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemService.findById(id))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Item");
    }

    @Test
    void findByCategoryDelegatesToRepository() {
        UUID categoryId = UUID.randomUUID();
        when(itemRepository.findAllByCategoryId(categoryId)).thenReturn(List.of());

        itemService.findByCategory(categoryId);

        verify(itemRepository).findAllByCategoryId(categoryId);
    }

    @Test
    void findCategoryTreeUsesRootCategories() {
        when(itemCategoryRepository.findAllByParentIsNullOrderBySortOrderAscNameAsc()).thenReturn(List.of());

        itemService.findCategoryTree();

        verify(itemMapper).toCategoryDtoList(List.of());
    }
}
