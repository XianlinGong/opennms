package org.opennms.features.topology.plugins.topo.odl;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.opennms.core.xml.JaxbUtils;
import org.opennms.features.topology.api.browsers.ContentType;
import org.opennms.features.topology.api.browsers.SelectionChangedListener;
import org.opennms.features.topology.api.browsers.SelectionChangedListener.Selection;
import org.opennms.features.topology.api.support.VertexHopGraphProvider;
import org.opennms.features.topology.api.topo.AbstractEdge;
import org.opennms.features.topology.api.topo.AbstractTopologyProvider;
import org.opennms.features.topology.api.topo.AbstractVertex;
import org.opennms.features.topology.api.topo.Defaults;
import org.opennms.features.topology.api.topo.GraphProvider;
import org.opennms.features.topology.api.topo.SimpleEdgeProvider;
import org.opennms.features.topology.api.topo.SimpleVertexProvider;
import org.opennms.features.topology.api.topo.Vertex;
import org.opennms.features.topology.api.topo.VertexRef;
import org.opennms.integrations.odl.NamingUtils;
import org.opennms.integrations.odl.topo.Link;
import org.opennms.integrations.odl.topo.Links;
import org.opennms.netmgt.dao.api.NodeDao;
import org.opennms.netmgt.model.OnmsAssetRecord;
import org.opennms.netmgt.model.OnmsNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class OpendaylightTopologyProvider extends AbstractTopologyProvider implements GraphProvider {

    public static final String TOPOLOGY_NAMESPACE_ODL = "odl";

    private static final Logger LOG = LoggerFactory.getLogger(OpendaylightTopologyProvider.class);

    private NodeDao m_nodeDao;

    public OpendaylightTopologyProvider() {
        super(new SimpleVertexProvider(TOPOLOGY_NAMESPACE_ODL), new SimpleEdgeProvider(TOPOLOGY_NAMESPACE_ODL));
    }

    @Override
    public Selection getSelection(List<VertexRef> selectedVertices, ContentType type) {
        return SelectionChangedListener.Selection.NONE;
    }

    @Override
    public boolean contributesTo(ContentType type) {
        return Sets.newHashSet(ContentType.Alarm, ContentType.Node).contains(type);
    }

    private void load() {
        LOG.debug("Loading...");
        List<Link> allLinks = Lists.newArrayList();
        resetContainer();
        for (OnmsNode node : m_nodeDao.findAll()) {
            try {
                //String topologyId = NamingUtils.getTopologyIdFromForeignId(node.getForeignId());
                String nodeId = NamingUtils.getNodeIdFromForeignId(node.getForeignId());

                // Create the vertex
                AbstractVertex vertex = addVertex(nodeId, 50, 50);
                vertex.setLabel(node.getLabel());
                vertex.setNodeID(node.getId());
                if (node.getLabel().startsWith("host")) {
                    vertex.setIconKey("HOST");
                } else if (node.getLabel().startsWith("openflow")) {
                    vertex.setIconKey("OPENFLOW_SWITCH");
                }

                // Grab the links
                OnmsAssetRecord assets = node.getAssetRecord();
                String topologyInfo = assets.getVmwareTopologyInfo();
                if (topologyInfo == null) {
                    continue;
                }

                Links links = JaxbUtils.unmarshal(Links.class, topologyInfo);
                allLinks.addAll(links.getLinks());
            } catch (Throwable t) {
                LOG.warn("Something failed on {}.", node, t);
                continue;
            }
        }

        // Create the edges
        for (Link link : allLinks) {
            Vertex source = getVertex(getNamespace(), link.getSourceNode());
            Vertex target = getVertex(getNamespace(), link.getDestinationNode());
            if (source == null || target == null) {
                continue;
            }
            addEdges(new AbstractEdge(getNamespace(), link.getId(), source, target));
        }
        LOG.debug("Loaded.");
    }

    @Override
    public void refresh() {
        load();
    }

    public void setNodeDao(NodeDao nodeDao) {
        m_nodeDao = Objects.requireNonNull(nodeDao);
    }

    @Override
    public Defaults getDefaults() {
        return new Defaults()
                .withSemanticZoomLevel(2)
                .withCriteria(() -> {
                    final Optional<Vertex> firstOpenflowSwitch = getSimpleVertexProvider().getVertices().stream()
                        .filter(v -> "OPENFLOW_SWITCH".equals(v.getIconKey()))
                        .findFirst();

                    if (!firstOpenflowSwitch.isPresent()) {
                        return null;
                    }
                    return Lists.newArrayList(new VertexHopGraphProvider.DefaultVertexHopCriteria(firstOpenflowSwitch.get()));
                });
    }
}
