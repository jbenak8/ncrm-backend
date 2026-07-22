package cz.jbenak.ncrm_backend.services;

import cz.jbenak.ncrm_backend.model.dto.store.ItemCategoryRequest;
import cz.jbenak.ncrm_backend.model.dto.store.ItemDto;
import cz.jbenak.ncrm_backend.model.dto.store.ItemRequest;
import cz.jbenak.ncrm_backend.model.entity.store.ItemCategoryEntity;
import cz.jbenak.ncrm_backend.model.entity.store.ItemEntity;
import cz.jbenak.ncrm_backend.model.mapper.ItemMapper;
import cz.jbenak.ncrm_backend.repository.ItemCategoryRepository;
import cz.jbenak.ncrm_backend.repository.ItemRepository;
import cz.jbenak.ncrm_backend.repository.OrderItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
    private OrderItemRepository orderItemRepository;
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

    @Test
    void uploadImageStoresContentAndContentType() {
        UUID id = UUID.randomUUID();
        ItemEntity item = new ItemEntity();
        when(itemRepository.findById(id)).thenReturn(Optional.of(item));
        when(itemRepository.save(item)).thenReturn(item);
        MockMultipartFile file = new MockMultipartFile("file", "item.png", "image/png", new byte[]{1, 2, 3});

        itemService.uploadImage(id, file);

        assertThat(item.getImage()).containsExactly(1, 2, 3);
        assertThat(item.getImageContentType()).isEqualTo("image/png");
        verify(itemRepository).save(item);
    }

    @Test
    void uploadImageRejectsEmptyFile() {
        UUID id = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "item.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> itemService.uploadImage(id, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void uploadImageRejectsUnsupportedContentType() {
        UUID id = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "item.pdf", "application/pdf", new byte[]{1});

        assertThatThrownBy(() -> itemService.uploadImage(id, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported image content type");
    }

    @Test
    void uploadImageRejectsTooLargeFile() {
        UUID id = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "item.png", "image/png", new byte[2 * 1024 * 1024 + 1]);

        assertThatThrownBy(() -> itemService.uploadImage(id, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maximum allowed size");
    }

    @Test
    void getImageReturnsStoredImage() {
        UUID id = UUID.randomUUID();
        ItemEntity item = new ItemEntity();
        item.setImage(new byte[]{4, 5});
        item.setImageContentType("image/jpeg");
        when(itemRepository.findById(id)).thenReturn(Optional.of(item));

        var image = itemService.getImage(id);

        assertThat(image.content()).containsExactly(4, 5);
        assertThat(image.contentType()).isEqualTo("image/jpeg");
    }

    @Test
    void getImageThrowsWhenItemHasNoImage() {
        UUID id = UUID.randomUUID();
        when(itemRepository.findById(id)).thenReturn(Optional.of(new ItemEntity()));

        assertThatThrownBy(() -> itemService.getImage(id))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("no image");
    }

    @Test
    void searchDelegatesToRepositoryWithSpecification() {
        Page<ItemEntity> page = new PageImpl<>(List.of(new ItemEntity()));
        when(itemRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(page);
        when(itemMapper.toDto(any(ItemEntity.class))).thenReturn(mock(ItemDto.class));

        Page<ItemDto> result = itemService.search(List.of("code:eq:P-01"), PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void searchRejectsNonSearchableField() {
        List<String> criteria = List.of("image:eq:x");
        PageRequest pageable = PageRequest.of(0, 10);
        assertThatThrownBy(() -> itemService.search(criteria, pageable))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("image");
    }

    @Test
    void createItemStoresAllFieldsIncludingPrice() {
        UUID categoryId = UUID.randomUUID();
        ItemCategoryEntity category = ItemCategoryEntity.builder().id(categoryId).code("1").path("1").build();
        when(itemCategoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(itemRepository.save(any(ItemEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        itemService.createItem(itemRequest(categoryId));

        ArgumentCaptor<ItemEntity> captor = ArgumentCaptor.forClass(ItemEntity.class);
        verify(itemRepository).save(captor.capture());
        ItemEntity saved = captor.getValue();
        assertThat(saved.getCode()).isEqualTo("P-01");
        assertThat(saved.getName()).isEqualTo("Papír A4");
        assertThat(saved.getCategory()).isSameAs(category);
        assertThat(saved.getPrice()).isNotNull();
        assertThat(saved.getPrice().getPrice()).isEqualByComparingTo("100.00");
        assertThat(saved.getPrice().getCurrency()).isEqualTo("CZK");
        assertThat(saved.getPrice().getPurchasePriceNet()).isEqualByComparingTo("75.00");
        assertThat(saved.getPrice().getVatRate()).isEqualByComparingTo("21.00");
        assertThat(saved.getPrice().getValidFrom()).isNotNull();
    }

    @Test
    void createItemFailsWhenCategoryDoesNotExist() {
        UUID categoryId = UUID.randomUUID();
        when(itemCategoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        ItemRequest request = itemRequest(categoryId);
        assertThatThrownBy(() -> itemService.createItem(request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("ItemCategory");
    }

    @Test
    void updateItemOverwritesFieldsAndReusesPriceRecord() {
        UUID id = UUID.randomUUID();
        ItemEntity item = new ItemEntity();
        cz.jbenak.ncrm_backend.model.entity.store.ItemPriceEntity price =
                cz.jbenak.ncrm_backend.model.entity.store.ItemPriceEntity.builder().item(item).build();
        item.setPrice(price);
        when(itemRepository.findById(id)).thenReturn(Optional.of(item));
        when(itemRepository.save(item)).thenReturn(item);

        itemService.updateItem(id, itemRequest(null));

        assertThat(item.getCode()).isEqualTo("P-01");
        assertThat(item.getCategory()).isNull();
        assertThat(item.getPrice()).isSameAs(price);
        assertThat(price.getPrice()).isEqualByComparingTo("100.00");
    }

    @Test
    void deleteItemRemovesUnreferencedItem() {
        UUID id = UUID.randomUUID();
        ItemEntity item = new ItemEntity();
        when(itemRepository.findById(id)).thenReturn(Optional.of(item));
        when(orderItemRepository.existsByItemId(id)).thenReturn(false);

        var result = itemService.deleteItem(id);

        assertThat(result.deleted()).isTrue();
        assertThat(result.deactivated()).isFalse();
        verify(itemRepository).delete(item);
    }

    @Test
    void deleteItemOnlyDeactivatesItemReferencedByOrders() {
        UUID id = UUID.randomUUID();
        ItemEntity item = new ItemEntity();
        item.setActive(true);
        when(itemRepository.findById(id)).thenReturn(Optional.of(item));
        when(orderItemRepository.existsByItemId(id)).thenReturn(true);

        var result = itemService.deleteItem(id);

        assertThat(result.deleted()).isFalse();
        assertThat(result.deactivated()).isTrue();
        assertThat(item.isActive()).isFalse();
        verify(itemRepository).save(item);
        verify(itemRepository, never()).delete(item);
    }

    @Test
    void createCategoryBuildsMaterializedPathFromParent() {
        UUID parentId = UUID.randomUUID();
        ItemCategoryEntity parent = ItemCategoryEntity.builder().id(parentId).code("1").path("1").build();
        when(itemCategoryRepository.findById(parentId)).thenReturn(Optional.of(parent));
        when(itemCategoryRepository.save(any(ItemCategoryEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        itemService.createCategory(new ItemCategoryRequest("2", "Hygienic", null, parentId, 1, true));

        ArgumentCaptor<ItemCategoryEntity> captor = ArgumentCaptor.forClass(ItemCategoryEntity.class);
        verify(itemCategoryRepository).save(captor.capture());
        assertThat(captor.getValue().getPath()).isEqualTo("1.2");
        assertThat(captor.getValue().getParent()).isSameAs(parent);
    }

    @Test
    void createRootCategoryUsesOwnCodeAsPath() {
        when(itemCategoryRepository.save(any(ItemCategoryEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        itemService.createCategory(new ItemCategoryRequest("1", "Paper", null, null, 0, true));

        ArgumentCaptor<ItemCategoryEntity> captor = ArgumentCaptor.forClass(ItemCategoryEntity.class);
        verify(itemCategoryRepository).save(captor.capture());
        assertThat(captor.getValue().getPath()).isEqualTo("1");
        assertThat(captor.getValue().getParent()).isNull();
    }

    @Test
    void updateCategoryRecomputesChildPaths() {
        UUID id = UUID.randomUUID();
        ItemCategoryEntity category = ItemCategoryEntity.builder().id(id).code("2").path("2").build();
        ItemCategoryEntity child = ItemCategoryEntity.builder().id(UUID.randomUUID()).code("1").path("2.1").build();
        category.addChild(child);
        when(itemCategoryRepository.findById(id)).thenReturn(Optional.of(category));
        when(itemCategoryRepository.save(category)).thenReturn(category);

        itemService.updateCategory(id, new ItemCategoryRequest("3", "Steel", null, null, 0, true));

        assertThat(category.getPath()).isEqualTo("3");
        assertThat(child.getPath()).isEqualTo("3.1");
    }

    @Test
    void updateCategoryRejectsMoveUnderItself() {
        UUID id = UUID.randomUUID();
        ItemCategoryEntity category = ItemCategoryEntity.builder().id(id).code("1").path("1").build();
        when(itemCategoryRepository.findById(id)).thenReturn(Optional.of(category));

        ItemCategoryRequest request = new ItemCategoryRequest("1", "Paper", null, id, 0, true);
        assertThatThrownBy(() -> itemService.updateCategory(id, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("under itself");
    }

    @Test
    void updateCategoryRejectsMoveUnderOwnSubtree() {
        UUID id = UUID.randomUUID();
        UUID childId = UUID.randomUUID();
        ItemCategoryEntity category = ItemCategoryEntity.builder().id(id).code("1").path("1").build();
        ItemCategoryEntity child = ItemCategoryEntity.builder().id(childId).code("2").path("1.2").build();
        when(itemCategoryRepository.findById(id)).thenReturn(Optional.of(category));
        when(itemCategoryRepository.findById(childId)).thenReturn(Optional.of(child));

        ItemCategoryRequest request = new ItemCategoryRequest("1", "Paper", null, childId, 0, true);
        assertThatThrownBy(() -> itemService.updateCategory(id, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("subtree");
    }

    @Test
    void deleteCategoryAsksForConfirmationWhenSubtreeContainsItems() {
        UUID id = UUID.randomUUID();
        ItemCategoryEntity category = ItemCategoryEntity.builder().id(id).code("1").path("1").build();
        when(itemCategoryRepository.findById(id)).thenReturn(Optional.of(category));
        when(itemCategoryRepository.findAllByPathStartingWith("1")).thenReturn(List.of(category));
        when(itemRepository.findAllByCategoryIdIn(List.of(id))).thenReturn(List.of(new ItemEntity()));

        var result = itemService.deleteCategory(id, false);

        assertThat(result.deleted()).isFalse();
        assertThat(result.requiresConfirmation()).isTrue();
        assertThat(result.affectedItems()).isEqualTo(1);
        verify(itemCategoryRepository, never()).delete(category);
    }

    @Test
    void deleteCategoryForcedDeletesOrDeactivatesItems() {
        UUID id = UUID.randomUUID();
        ItemCategoryEntity category = ItemCategoryEntity.builder().id(id).code("1").path("1").build();
        ItemEntity referenced = new ItemEntity();
        referenced.setId(UUID.randomUUID());
        referenced.setActive(true);
        ItemEntity unreferenced = new ItemEntity();
        unreferenced.setId(UUID.randomUUID());
        when(itemCategoryRepository.findById(id)).thenReturn(Optional.of(category));
        when(itemCategoryRepository.findAllByPathStartingWith("1")).thenReturn(List.of(category));
        when(itemRepository.findAllByCategoryIdIn(List.of(id))).thenReturn(List.of(referenced, unreferenced));
        when(orderItemRepository.existsByItemId(referenced.getId())).thenReturn(true);
        when(orderItemRepository.existsByItemId(unreferenced.getId())).thenReturn(false);

        var result = itemService.deleteCategory(id, true);

        assertThat(result.deleted()).isTrue();
        assertThat(result.deletedItems()).isEqualTo(1);
        assertThat(result.deactivatedItems()).isEqualTo(1);
        assertThat(referenced.isActive()).isFalse();
        assertThat(referenced.getCategory()).isNull();
        verify(itemRepository).delete(unreferenced);
        verify(itemCategoryRepository).delete(category);
    }

    @Test
    void deleteCategoryIgnoresSiblingsWithSimilarPathPrefix() {
        UUID id = UUID.randomUUID();
        ItemCategoryEntity category = ItemCategoryEntity.builder().id(id).code("1").path("1").build();
        ItemCategoryEntity sibling = ItemCategoryEntity.builder().id(UUID.randomUUID()).code("10").path("10").build();
        when(itemCategoryRepository.findById(id)).thenReturn(Optional.of(category));
        when(itemCategoryRepository.findAllByPathStartingWith("1")).thenReturn(List.of(category, sibling));
        when(itemRepository.findAllByCategoryIdIn(List.of(id))).thenReturn(List.of());

        var result = itemService.deleteCategory(id, false);

        assertThat(result.deleted()).isTrue();
        verify(itemCategoryRepository).delete(category);
        verify(itemCategoryRepository, never()).delete(sibling);
    }

    private ItemRequest itemRequest(UUID categoryId) {
        return new ItemRequest("P-01", "Papír A4", "Kancelářský papír", ItemEntity.ItemType.GOODS,
                categoryId, "ks", true, new BigDecimal("100.00"), "CZK", new BigDecimal("75.00"),
                new BigDecimal("21.00"));
    }

    @Test
    void deleteImageClearsImageFields() {
        UUID id = UUID.randomUUID();
        ItemEntity item = new ItemEntity();
        item.setImage(new byte[]{1});
        item.setImageContentType("image/png");
        when(itemRepository.findById(id)).thenReturn(Optional.of(item));
        when(itemRepository.save(item)).thenReturn(item);
        when(itemMapper.toDto(any(ItemEntity.class))).thenReturn(mock(ItemDto.class));

        itemService.deleteImage(id);

        assertThat(item.getImage()).isNull();
        assertThat(item.getImageContentType()).isNull();
        verify(itemRepository).save(item);
    }
}
