package dev.quang.portfolio.controller;

import dev.quang.portfolio.dto.NodeDtos.*;
import dev.quang.portfolio.service.NodeDataSeeder;
import dev.quang.portfolio.service.NodeService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class NodeController {

    private final NodeService nodeService;
    private final NodeDataSeeder nodeDataSeeder;

    public NodeController(NodeService nodeService, NodeDataSeeder nodeDataSeeder) {
        this.nodeService = nodeService;
        this.nodeDataSeeder = nodeDataSeeder;
    }

    @GetMapping("/api/nodes")
    public List<ContentNodeDto> getPublicTree() {
        return nodeService.getTree();
    }

    @PostMapping("/api/admin/nodes")
    @ResponseStatus(HttpStatus.CREATED)
    public ContentNodeDto createNode(@Valid @RequestBody SaveNodeRequest request) {
        return nodeService.saveNode(null, request);
    }

    @PutMapping("/api/admin/nodes/{id}")
    public ContentNodeDto updateNode(
            @PathVariable String id,
            @Valid @RequestBody SaveNodeRequest request
    ) {
        return nodeService.saveNode(id, request);
    }

    @DeleteMapping("/api/admin/nodes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteNode(@PathVariable String id) {
        nodeService.deleteNode(id);
    }

    @PutMapping("/api/admin/nodes/reorder")
    public List<ContentNodeDto> reorderNodes(@Valid @RequestBody ReorderNodesRequest request) {
        return nodeService.reorderNodes(request);
    }

    @PutMapping("/api/admin/nodes/changes")
    public List<ContentNodeDto> applyChanges(@Valid @RequestBody BlockChangeRequest request) {
        return nodeService.applyChanges(request);
    }

    @PostMapping("/api/admin/nodes/reset")
    public List<ContentNodeDto> resetDefaultTree() {
        nodeDataSeeder.resetDefaultTree();
        return nodeService.getTree();
    }
}
