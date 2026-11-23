package dev.quang.portfolio.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.quang.portfolio.dto.NodeDtos.*;
import dev.quang.portfolio.entity.ContentNodeEntity;
import dev.quang.portfolio.repository.ContentNodeRepository;
import java.time.OffsetDateTime;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NodeService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final ContentNodeRepository nodeRepository;
    private final ObjectMapper objectMapper;

    public NodeService(ContentNodeRepository nodeRepository, ObjectMapper objectMapper) {
        this.nodeRepository = nodeRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<ContentNodeDto> getTree() {
        List<ContentNodeEntity> all = nodeRepository.findAllByOrderBySortOrderAscIdAsc();
        Map<String, List<ContentNodeEntity>> byParent = new LinkedHashMap<>();
        for (ContentNodeEntity entity : all) {
            String key = entity.getParentId() == null ? "__ROOT__" : entity.getParentId();
            byParent.computeIfAbsent(key, k -> new ArrayList<>()).add(entity);
        }
        return buildChildren("__ROOT__", byParent, new HashSet<>());
    }

    private List<ContentNodeDto> buildChildren(
            String parentKey,
            Map<String, List<ContentNodeEntity>> byParent,
            Set<String> visited
    ) {
        List<ContentNodeEntity> list = byParent.getOrDefault(parentKey, List.of());
        List<ContentNodeDto> result = new ArrayList<>(list.size());
        for (ContentNodeEntity entity : list) {
            if (!visited.add(entity.getId())) {
                continue;
            }
            List<ContentNodeDto> children = buildChildren(entity.getId(), byParent, visited);
            result.add(toDto(entity, children));
        }
        return result;
    }

    @Transactional
    public ContentNodeDto saveNode(String pathId, SaveNodeRequest request) {
        String id = pathId != null && !pathId.isBlank()
                ? pathId.trim()
                : (request.id() != null && !request.id().isBlank()
                ? request.id().trim()
                : "node-" + UUID.randomUUID().toString().substring(0, 8));

        String parentId = request.parentId() != null && !request.parentId().isBlank()
                ? request.parentId().trim()
                : null;

        if (id.equals(parentId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A node cannot be its own parent");
        }
        if (parentId != null) {
            if (!nodeRepository.existsById(parentId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parent node not found: " + parentId);
            }
            if (isDescendant(id, parentId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot move a node inside its own descendant");
            }
        }

        ContentNodeEntity entity = nodeRepository.findById(id).orElseGet(ContentNodeEntity::new);
        boolean isNew = entity.getId() == null;
        entity.setId(id);
        entity.setParentId(parentId);
        entity.setTag(request.tag().trim().toLowerCase(Locale.ROOT));

        if (request.beforeId() != null) {
            if (!isNew) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insert position is only available for new blocks");
            }
            entity.setSortOrder(makeRoomBefore(parentId, request.beforeId()));
        } else if (request.sortOrder() != null) {
            entity.setSortOrder(request.sortOrder());
        } else if (isNew) {
            List<ContentNodeEntity> siblings = nodeRepository.findByParentIdOrderBySortOrderAscIdAsc(parentId);
            int maxOrder = siblings.stream().mapToInt(ContentNodeEntity::getSortOrder).max().orElse(-1);
            entity.setSortOrder(maxOrder + 1);
        }

        entity.setAttrsJson(writeAttrs(request.attrs()));
        entity.setContentVi(request.contentVi() != null ? request.contentVi() : "");
        entity.setContentEn(request.contentEn() != null ? request.contentEn() : "");
        entity.setUpdatedAt(OffsetDateTime.now());

        ContentNodeEntity saved = nodeRepository.save(entity);
        return toDto(saved, List.of());
    }

    private int makeRoomBefore(String parentId, String beforeId) {
        List<ContentNodeEntity> siblings = nodeRepository.findByParentIdOrderBySortOrderAscIdAsc(parentId);
        int insertionIndex = 0;
        while (insertionIndex < siblings.size() && !siblings.get(insertionIndex).getId().equals(beforeId)) {
            insertionIndex++;
        }
        if (insertionIndex == siblings.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insert position is no longer available");
        }
        for (int i = 0; i < siblings.size(); i++) {
            siblings.get(i).setSortOrder(i < insertionIndex ? i : i + 1);
        }
        return insertionIndex;
    }

    @Transactional
    public void deleteNode(String id) {
        if (!nodeRepository.existsById(id)) {
            throw new NoSuchElementException("Node not found: " + id);
        }
        // Delete descendants explicitly for H2/PostgreSQL compatibility regardless of FK cascade quirks
        deleteRecursive(id);
    }

    private void deleteRecursive(String id) {
        List<ContentNodeEntity> children = nodeRepository.findByParentIdOrderBySortOrderAscIdAsc(id);
        for (ContentNodeEntity child : children) {
            deleteRecursive(child.getId());
        }
        nodeRepository.deleteById(id);
    }

    @Transactional
    public List<ContentNodeDto> reorderNodes(ReorderNodesRequest request) {
        if (request == null || request.items() == null || request.items().isEmpty()) {
            return getTree();
        }
        for (NodeReorderItem item : request.items()) {
            ContentNodeEntity entity = nodeRepository.findById(item.id())
                    .orElseThrow(() -> new NoSuchElementException("Node not found: " + item.id()));
            String newParent = item.parentId() != null && !item.parentId().isBlank() ? item.parentId().trim() : null;
            if (entity.getId().equals(newParent)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A node cannot be its own parent");
            }
            entity.setParentId(newParent);
            entity.setSortOrder(item.sortOrder());
            if (item.clearGridPosition()) {
                Map<String, Object> attrs = new LinkedHashMap<>(readAttrs(entity.getAttrsJson()));
                attrs.remove("colStart");
                if (attrs.get("responsive") instanceof Map<?, ?> variants) {
                    for (Object variant : variants.values()) {
                        if (variant instanceof Map<?, ?> layout) layout.remove("colStart");
                    }
                }
                entity.setAttrsJson(writeAttrs(attrs));
            }
            entity.setUpdatedAt(OffsetDateTime.now());
            nodeRepository.save(entity);
        }
        return getTree();
    }

    private boolean isDescendant(String ancestorId, String candidateId) {
        String current = candidateId;
        Set<String> seen = new HashSet<>();
        while (current != null && seen.add(current)) {
            if (current.equals(ancestorId)) {
                return true;
            }
            current = nodeRepository.findById(current)
                    .map(ContentNodeEntity::getParentId)
                    .orElse(null);
        }
        return false;
    }

    @Transactional
    public List<ContentNodeDto> applyChanges(BlockChangeRequest request) {
        Map<String, BlockState> expected = indexStates(request.expected());
        Map<String, BlockState> replacement = indexStates(request.replacement());
        Set<String> changed = new LinkedHashSet<>(expected.keySet());
        changed.addAll(replacement.keySet());

        Map<String, ContentNodeEntity> entities = new HashMap<>();
        Map<String, String> parents = new HashMap<>();
        for (ContentNodeEntity node : nodeRepository.lockCanvasNodes()) {
            entities.put(node.getId(), node);
            parents.put(node.getId(), node.getParentId());
        }
        for (String id : changed) {
            ContentNodeEntity node = entities.get(id);
            BlockState current = node == null ? null : new BlockState(id, node.getParentId(), node.getTag(),
                    node.getSortOrder(), readAttrs(node.getAttrsJson()), node.getContentVi(), node.getContentEn());
            var currentJson = objectMapper.valueToTree(current);
            var expectedJson = objectMapper.valueToTree(expected.get(id));
            if (!currentJson.equals((a, b) -> a.isNumber() && b.isNumber()
                    ? a.decimalValue().compareTo(b.decimalValue()) : a.equals(b) ? 0 : 1, expectedJson)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "This block has changed. Refresh the page and retry.");
            }
            BlockState next = replacement.get(id);
            if (next == null) parents.remove(id);
            else parents.put(id, next.parentId());
        }

        List<String> ordered = parentFirst(parents);
        for (String id : ordered) {
            BlockState state = replacement.get(id);
            if (state == null) continue;
            ContentNodeEntity node = entities.getOrDefault(id, new ContentNodeEntity());
            node.setId(state.id());
            node.setParentId(state.parentId());
            node.setTag(state.tag());
            node.setSortOrder(state.sortOrder());
            node.setAttrsJson(writeAttrs(state.attrs()));
            node.setContentVi(state.contentVi());
            node.setContentEn(state.contentEn());
            node.setUpdatedAt(OffsetDateTime.now());
            nodeRepository.saveAndFlush(node);
        }
        List<String> removed = changed.stream().filter(id -> !replacement.containsKey(id)).toList();
        if (!removed.isEmpty()) nodeRepository.deleteAllByIdInBatch(removed);
        return getTree();
    }

    private Map<String, BlockState> indexStates(List<BlockState> states) {
        Map<String, BlockState> indexed = new LinkedHashMap<>();
        for (BlockState state : states) {
            if (indexed.putIfAbsent(state.id(), state) != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate block in changes");
            }
        }
        return indexed;
    }

    private List<String> parentFirst(Map<String, String> parents) {
        Set<String> visited = new HashSet<>();
        List<String> ordered = new ArrayList<>();
        for (String id : parents.keySet()) {
            Deque<String> path = new ArrayDeque<>();
            Set<String> ancestors = new HashSet<>();
            String ancestor = id;
            while (ancestor != null && !visited.contains(ancestor)) {
                if (!parents.containsKey(ancestor) || !ancestors.add(ancestor)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "The changes would leave an invalid block hierarchy");
                }
                path.push(ancestor);
                ancestor = parents.get(ancestor);
            }
            visited.addAll(path);
            ordered.addAll(path);
        }
        return ordered;
    }

    private ContentNodeDto toDto(ContentNodeEntity entity, List<ContentNodeDto> children) {
        return new ContentNodeDto(
                entity.getId(),
                entity.getParentId(),
                entity.getTag(),
                entity.getSortOrder(),
                readAttrs(entity.getAttrsJson()),
                entity.getContentVi(),
                entity.getContentEn(),
                children
        );
    }

    private Map<String, Object> readAttrs(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private String writeAttrs(Map<String, Object> attrs) {
        if (attrs == null || attrs.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(attrs);
        } catch (Exception ex) {
            return "{}";
        }
    }
}
