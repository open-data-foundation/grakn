package grakn.core.graql.internal.reasoner.graph;

import grakn.core.GraknSession;
import grakn.core.GraknTx;
import grakn.core.GraknTxType;
import grakn.core.concept.Attribute;
import grakn.core.concept.AttributeType;
import grakn.core.concept.ConceptId;
import grakn.core.concept.EntityType;
import grakn.core.concept.Label;
import grakn.core.concept.RelationshipType;
import grakn.core.concept.Role;
import grakn.core.concept.Thing;
import grakn.core.graql.Query;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

@SuppressWarnings("CheckReturnValue")
public class TransitivityChainGraph {
    private final static Label key = Label.of("index");
    private final GraknSession session;

    public TransitivityChainGraph(GraknSession session) {
        this.session = session;
    }

    public void load(int n) {
        loadSchema();
        buildExtensionalDB(n);
    }

    private void loadSchema() {
        try {
            InputStream inputStream = TransitivityChainGraph.class.getClassLoader().getResourceAsStream("test-integration/test/graql/reasoner/resources/quadraticTransitivity.gql");
            String s = new BufferedReader(new InputStreamReader(inputStream)).lines().collect(Collectors.joining("\n"));
            GraknTx tx = session.transaction(GraknTxType.WRITE);
            tx.graql().parser().parseList(s).forEach(Query::execute);
            tx.commit();
        } catch (Exception e) {
            System.err.println(e);
            throw new RuntimeException(e);
        }
    }

    private void buildExtensionalDB(int n){
        GraknTx tx = session.transaction(GraknTxType.WRITE);
        Role qfrom = tx.getRole("Q-from");
        Role qto = tx.getRole("Q-to");

        EntityType aEntity = tx.getEntityType("a-entity");
        RelationshipType q = tx.getRelationshipType("Q");
        Thing aInst = putEntityWithResource(tx, "a", tx.getEntityType("entity2"), key);
        ConceptId[] aInstanceIds = new ConceptId[n];
        for(int i = 0 ; i < n ;i++) {
            aInstanceIds[i] = putEntityWithResource(tx, "a" + i, aEntity, key).id();
        }

        q.create()
                .assign(qfrom, aInst)
                .assign(qto, tx.getConcept(aInstanceIds[0]));

        for(int i = 0 ; i < n - 1 ; i++) {
            q.create()
                    .assign(qfrom, tx.getConcept(aInstanceIds[i]))
                    .assign(qto, tx.getConcept(aInstanceIds[i+1]));
        }
        tx.commit();
    }


    private static Thing putEntityWithResource(GraknTx tx, String id, EntityType type, Label key) {
        Thing inst = type.create();
        putResource(inst, tx.getSchemaConcept(key), id);
        return inst;
    }

    private static <T> void putResource(Thing thing, AttributeType<T> attributeType, T resource) {
        Attribute attributeInstance = attributeType.create(resource);
        thing.has(attributeInstance);
    }
}
