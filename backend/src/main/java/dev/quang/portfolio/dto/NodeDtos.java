package dev.quang.portfolio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

public class NodeDtos {

    public record ContentNodeDto(
            String id,
            String parentId,
            String tag,
            int sortOrder,
            Map<String, Object> attrs,
            String contentVi,
            String contentEn,
            List<ContentNodeDto> children
    ) {}

    public record SaveNodeRequest(
            @Size(max = 80) String id,
            @Size(max = 80) String parentId,
            @NotBlank @Size(max = 50) String tag,
            Integer sortOrder,
            Map<String, Object> attrs,
            String contentVi,
            String contentEn,
            @Size(max = 80) String beforeId
    ) {}

    public record NodeReorderItem(
            @NotBlank String id,
            String parentId,
            int sortOrder,
            boolean clearGridPosition
    ) {}

    public record ReorderNodesRequest(
            List<NodeReorderItem> items
    ) {}

    public record BlockState(
            @NotBlank @Size(max = 80) String id,
            @Size(max = 80) String parentId,
            @NotBlank @Size(max = 50) String tag,
            int sortOrder,
            @NotNull Map<String, Object> attrs,
            @NotNull String contentVi,
            @NotNull String contentEn
    ) {}

    public record BlockChangeRequest(
            @NotNull @Size(max = 500) List<@NotNull @Valid BlockState> expected,
            @NotNull @Size(max = 500) List<@NotNull @Valid BlockState> replacement
    ) {}
}
