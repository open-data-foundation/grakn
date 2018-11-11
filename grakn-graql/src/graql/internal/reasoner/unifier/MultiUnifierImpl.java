/*
 * GRAKN.AI - THE KNOWLEDGE GRAPH
 * Copyright (C) 2018 Grakn Labs Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package grakn.core.graql.internal.reasoner.unifier;

import grakn.core.exception.GraqlQueryException;
import grakn.core.graql.Var;
import grakn.core.graql.admin.MultiUnifier;
import grakn.core.graql.admin.Unifier;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.UnmodifiableIterator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;

/**
 *
 * <p>
 * Implementation of the {@link MultiUnifier} interface.
 *
 * Special cases:
 *
 * Identity unifier: contains a single trivial unifier.
 * Unifier doesn't exist: unifier is an empty set.
 *
 * </p>
 *
 *
 */
public class MultiUnifierImpl implements MultiUnifier{

    private final ImmutableSet<Unifier> multiUnifier;

    public MultiUnifierImpl(Set<Unifier> us){
        this.multiUnifier = ImmutableSet.copyOf(us);
    }
    public MultiUnifierImpl(Unifier u){
        this.multiUnifier = ImmutableSet.of(u);
    }

    /**
     * identity multiunifier: single trivial unifier
     */
    public MultiUnifierImpl(){ this.multiUnifier = ImmutableSet.of(new UnifierImpl()); }

    @SafeVarargs
    public MultiUnifierImpl(ImmutableMultimap<Var, Var>... maps){
        this.multiUnifier = ImmutableSet.<Unifier>builder()
                .addAll(Stream.of(maps).map(UnifierImpl::new).iterator())
                .build();
    }

    public static MultiUnifierImpl trivial(){ return new MultiUnifierImpl();}
    public static MultiUnifierImpl nonExistent(){ return new MultiUnifierImpl(new HashSet<>());}

    @Override
    public boolean equals(Object obj){
        if (obj == null || this.getClass() != obj.getClass()) return false;
        if (obj == this) return true;
        MultiUnifierImpl u2 = (MultiUnifierImpl) obj;
        return this.multiUnifier.equals(u2.multiUnifier);
    }

    @Override
    public int hashCode(){
        return multiUnifier.hashCode();
    }

    @Override
    public String toString(){ return multiUnifier.toString(); }

    @Override
    public Stream<Unifier> stream() {
        return multiUnifier.stream();
    }

    @Nonnull
    @Override
    public Iterator<Unifier> iterator() {
        return multiUnifier.iterator();
    }

    @Override
    public Unifier getUnifier() {
        return Iterables.getOnlyElement(multiUnifier);
    }

    @Override
    public Unifier getAny() {
        //TODO add a check it's a structural one
        UnmodifiableIterator<Unifier> iterator = multiUnifier.iterator();
        if (!iterator.hasNext()){
            throw GraqlQueryException.nonExistentUnifier();
        }
        return iterator.next();
    }

    @Override
    public ImmutableSet<Unifier> unifiers() { return multiUnifier;}

    @Override
    public boolean isEmpty() {
        return multiUnifier.isEmpty();
    }

    public boolean contains(Unifier u2) {
        return unifiers().stream().anyMatch(u -> u.containsAll(u2));
    }

    @Override
    public boolean containsAll(MultiUnifier mu) {
        return mu.unifiers().stream()
                .allMatch(this::contains);
    }

    @Override
    public MultiUnifier inverse() {
        return new MultiUnifierImpl(
                multiUnifier.stream()
                        .map(Unifier::inverse)
                        .collect(Collectors.toSet())
        );
    }

    @Override
    public int size() {
        return multiUnifier.size();
    }
}
