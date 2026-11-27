package dev.quang.portfolio.controller;

import dev.quang.portfolio.dto.NodeDtos.*;
import dev.quang.portfolio.security.AuthenticatedUser;
import dev.quang.portfolio.service.NodeDataSeeder;
import dev.quang.portfolio.service.NodeService;
import dev.quang.portfolio.service.SystemDataSeeder;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class NodeController {

    private final NodeService nodeService;
    private final NodeDataSeeder nodeDataSeeder;
    private final SystemDataSeeder systemDataSeeder;

    public NodeController(
            NodeService nodeService,
            NodeDataSeeder nodeDataSeeder,
            SystemDataSeeder systemDataSeeder
    ) {
        this.nodeService = nodeService;
        this.nodeDataSeeder = nodeDataSeeder;
        this.systemDataSeeder = systemDataSeeder;
    }

    // Legacy home-page node endpoints (kept for transition compatibility)
    @GetMapping("/api/nodes")
    public List<ContentNodeDto> getPublicTree() {
        return nodeService.getTreeForPage(systemDataSeeder.getDefaultPageId());
    }

    @PostMapping("/api/admin/nodes")
    @ResponseStatus(HttpStatus.CREATED)
    public ContentNodeDto createNode(@Valid @RequestBody SaveNodeRequest request) {
        return nodeService.saveNodeForPage(systemDataSeeder.getDefaultPageId(), null, request);
    }

    @PutMapping("/api/admin/nodes/{id}")
    public ContentNodeDto updateNode(
            @PathVariable String id,
            @Valid @RequestBody SaveNodeRequest request
    ) {
        return nodeService.saveNodeForPage(systemDataSeeder.getDefaultPageId(), id, request);
    }

    @DeleteMapping("/api/admin/nodes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteNode(@PathVariable String id) {
        nodeService.deleteNodeForPage(systemDataSeeder.getDefaultPageId(), id);
    }

    @PutMapping("/api/admin/nodes/reorder")
    public List<ContentNodeDto> reorderNodes(@Valid @RequestBody ReorderNodesRequest request) {
        return nodeService.reorderNodesForPage(systemDataSeeder.getDefaultPageId(), request);
    }

    @PutMapping("/api/admin/nodes/changes")
    public List<ContentNodeDto> applyChanges(@Valid @RequestBody BlockChangeRequest request) {
        return nodeService.applyChangesForPage(systemDataSeeder.getDefaultPageId(), request);
    }

    @PostMapping("/api/admin/nodes/reset")
    public List<ContentNodeDto> resetDefaultTree() {
        String defaultPageId = systemDataSeeder.getDefaultPageId();
        nodeDataSeeder.resetDefaultTreeForPage(defaultPageId, true);
        return nodeService.getTreeForPage(defaultPageId);
    }

    // Page-scoped Multi-tenant Node endpoints
    @GetMapping("/api/pages/{pageId}/nodes")
    public List<ContentNodeDto> getPageTree(
            Authentication authentication,
            @PathVariable String pageId
    ) {
        nodeService.requirePageEditAccess(pageId, AuthenticatedUser.require(authentication));
        return nodeService.getTreeForPage(pageId);
    }

    @PostMapping("/api/pages/{pageId}/nodes")
    @ResponseStatus(HttpStatus.CREATED)
    public ContentNodeDto createPageNode(
            Authentication authentication,
            @PathVariable String pageId,
            @Valid @RequestBody SaveNodeRequest request
    ) {
        nodeService.requirePageEditAccess(pageId, AuthenticatedUser.require(authentication));
        return nodeService.saveNodeForPage(pageId, null, request);
    }

    @PutMapping("/api/pages/{pageId}/nodes/{id}")
    public ContentNodeDto updatePageNode(
            Authentication authentication,
            @PathVariable String pageId,
            @PathVariable String id,
            @Valid @RequestBody SaveNodeRequest request
    ) {
        nodeService.requirePageEditAccess(pageId, AuthenticatedUser.require(authentication));
        return nodeService.saveNodeForPage(pageId, id, request);
    }

    @DeleteMapping("/api/pages/{pageId}/nodes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePageNode(
            Authentication authentication,
            @PathVariable String pageId,
            @PathVariable String id
    ) {
        nodeService.requirePageEditAccess(pageId, AuthenticatedUser.require(authentication));
        nodeService.deleteNodeForPage(pageId, id);
    }

    @PutMapping("/api/pages/{pageId}/nodes/reorder")
    public List<ContentNodeDto> reorderPageNodes(
            Authentication authentication,
            @PathVariable String pageId,
            @Valid @RequestBody ReorderNodesRequest request
    ) {
        nodeService.requirePageEditAccess(pageId, AuthenticatedUser.require(authentication));
        return nodeService.reorderNodesForPage(pageId, request);
    }

    @PutMapping("/api/pages/{pageId}/nodes/changes")
    public List<ContentNodeDto> applyPageChanges(
            Authentication authentication,
            @PathVariable String pageId,
            @Valid @RequestBody BlockChangeRequest request
    ) {
        nodeService.requirePageEditAccess(pageId, AuthenticatedUser.require(authentication));
        return nodeService.applyChangesForPage(pageId, request);
    }

    @PostMapping("/api/pages/{pageId}/nodes/reset")
    public List<ContentNodeDto> resetPageNodes(
            Authentication authentication,
            @PathVariable String pageId
    ) {
        nodeService.requirePageEditAccess(pageId, AuthenticatedUser.require(authentication));
        boolean preserveLegacyIds = pageId.equals(systemDataSeeder.getDefaultPageId());
        nodeDataSeeder.resetDefaultTreeForPage(pageId, preserveLegacyIds);
        return nodeService.getTreeForPage(pageId);
    }

    @PostMapping("/api/pages/{pageId}/nodes/import")
    public List<ContentNodeDto> importPageNodes(
            Authentication authentication,
            @PathVariable String pageId,
            @Valid @RequestBody List<BlockState> nodes
    ) {
        AuthenticatedUser user = AuthenticatedUser.require(authentication);
        nodeService.requirePageEditAccess(pageId, user);
        return nodeService.importNodesForPage(pageId, nodes);
    }
}
