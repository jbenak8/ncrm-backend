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
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
        MockMultipartFile file = new MockMultipartFile("file", "item.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> itemService.uploadImage(UUID.randomUUID(), file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void uploadImageRejectsUnsupportedContentType() {
        MockMultipartFile file = new MockMultipartFile("file", "item.pdf", "application/pdf", new byte[]{1});

        assertThatThrownBy(() -> itemService.uploadImage(UUID.randomUUID(), file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported image content type");
    }

    @Test
    void uploadImageRejectsTooLargeFile() {
        MockMultipartFile file = new MockMultipartFile("file", "item.png", "image/png", new byte[2 * 1024 * 1024 + 1]);

        assertThatThrownBy(() -> itemService.uploadImage(UUID.randomUUID(), file))
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
