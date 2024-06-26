/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
/**
 * <p>Provides the definition of the Relation Service.  The
 * Relation Service is used to record relationships between
 * MBeans in an MBean Server.  The Relation Service is itself an
 * MBean.  More than one instance of a {@link
 * javax.management.relation.RelationService RelationService}
 * MBean can be registered in an MBean Server.</p>
 *
 * <p>A <em>relation type</em> defines a relationship between MBeans.
 * It contains <em>roles</em> that the MBeans play in the
 * relationship.  Usually there are at least two roles in a
 * relation type.</p>
 *
 * <p>A <em>relation</em> is a named instance of a relation type,
 * where specific MBeans appear in the roles, represented by
 * their {@link javax.management.ObjectName ObjectName}s.</p>
 *
 * <p>For example, suppose there are <code>Module</code> MBeans,
 * representing modules within an application.  A
 * <code>DependsOn</code> relation type could express the
 * relationship that some modules depend on others, which could
 * be used to determine the order in which the modules are
 * started or stopped.  The <code>DependsOn</code> relation type
 * would have two roles, <code>dependent</code> and
 * <code>dependedOn</code>.</p>
 *
 * <p>Every role is <em>typed</em>, meaning that an MBean that
 * appears in that role must be an instance of the role's type.
 * In the <code>DependsOn</code> example, both roles would be of
 * type <code>Module</code>.</p>
 *
 * <p>Every role has a <em>cardinality</em>, which provides lower
 * and upper bounds on the number of MBeans that can appear in
 * that role in a given relation instance.  Usually, the lower
 * and upper bounds are both 1, with exactly one MBean appearing
 * in the role.  The cardinality only limits the number of MBeans
 * in the role per relation instance.  The same MBean can appear
 * in the same role in any number of instances of a relation
 * type.  In the <code>DependsOn</code> example, a given module
 * can depend on many other modules, and be depended on by many
 * others, but any given relation instance links exactly one
 * <code>dependent</code> module with exactly one
 * <code>dependedOn</code> module.</p>
 *
 * <p>A relation type can be created explicitly, as an object
 * implementing the {@link javax.management.relation.RelationType
 * RelationType} interface, typically a {@link
 * javax.management.relation.RelationTypeSupport
 * RelationTypeSupport}.  Alternatively, it can be created
 * implicitly using the Relation Service's {@link
 * javax.management.relation.RelationServiceMBean#createRelationType(String,
 * RoleInfo[]) createRelationType} method.</p>
 *
 * <p>A relation instance can be created explicitly, as an object
 * implementing the {@link javax.management.relation.Relation
 * Relation} interface, typically a {@link
 * javax.management.relation.RelationSupport RelationSupport}.
 * (A <code>RelationSupport</code> is itself a valid MBean, so it
 * can be registered in the MBean Server, though this is not
 * required.)  Alternatively, a relation instance can be created
 * implicitly using the Relation Service's {@link
 * javax.management.relation.RelationServiceMBean#createRelation(String,
 * String, RoleList) createRelation} method.</p>
 *
 * <p>The <code>DependsOn</code> example might be coded as follows.</p>
 *
 * <pre>
 * import java.util.*;
 * import javax.management.*;
 * import javax.management.relation.*;
 *
 * // ...
 * MBeanServer mbs = ...;
 *
 * // Create the Relation Service MBean
 * ObjectName relSvcName = new ObjectName(":type=RelationService");
 * RelationService relSvcObject = new RelationService(true);
 * mbs.registerMBean(relSvcObject, relSvcName);
 *
 * // Create an MBean proxy for easier access to the Relation Service
 * RelationServiceMBean relSvc =
 *     MBeanServerInvocationHandler.newProxyInstance(mbs, relSvcName,
 *                           RelationServiceMBean.class,
 *                           false);
 *
 * // Define the DependsOn relation type
 * RoleInfo[] dependsOnRoles = {
 *     new RoleInfo("dependent", Module.class.getName()),
 *     new RoleInfo("dependedOn", Module.class.getName())
 * };
 * relSvc.createRelationType("DependsOn", dependsOnRoles);
 *
 * // Now define a relation instance "moduleA DependsOn moduleB"
 *
 * ObjectName moduleA = new ObjectName(":type=Module,name=A");
 * ObjectName moduleB = new ObjectName(":type=Module,name=B");
 *
 * Role dependent = new Role("dependent", Collections.singletonList(moduleA));
 * Role dependedOn = new Role("dependedOn", Collections.singletonList(moduleB));
 * Role[] roleArray = {dependent, dependedOn};
 * RoleList roles = new RoleList(Arrays.asList(roleArray));
 * relSvc.createRelation("A-DependsOn-B", "DependsOn", roles);
 *
 * // Query the Relation Service to find what modules moduleA depends on
 * Map&lt;ObjectName,List&lt;String&gt;&gt; dependentAMap =
 *     relSvc.findAssociatedMBeans(moduleA, "DependsOn", "dependent");
 * Set&lt;ObjectName&gt; dependentASet = dependentAMap.keySet();
 * // Set of ObjectName containing moduleB
 * </pre>
 *
 * @see <a href="https://jcp.org/aboutJava/communityprocess/mrel/jsr160/index2.html">
 * JMX Specification, version 1.4</a>
 *
 * @since 1.5
 */
package javax.management.relation;
