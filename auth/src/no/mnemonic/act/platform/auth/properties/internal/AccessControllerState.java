package no.mnemonic.act.platform.auth.properties.internal;

import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.MapUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Class holding the state of the AccessController implementation and providing helper methods to access the state.
 */
public class AccessControllerState {

  private final Map<String, Function> functionMap;
  private final Map<Long, Organization> organizationMap;
  private final Map<String, Organization> organizationByNameMap;
  private final Map<Long, Subject> subjectMap;

  private AccessControllerState(Map<String, Function> functionMap, Map<Long, Organization> organizationMap, Map<Long, Subject> subjectMap) {
    this.functionMap = ObjectUtils.ifNotNull(functionMap, Collections::unmodifiableMap, Collections.emptyMap());
    this.organizationMap = ObjectUtils.ifNotNull(organizationMap, Collections::unmodifiableMap, Collections.emptyMap());
    this.organizationByNameMap = Collections.unmodifiableMap(MapUtils.map(this.organizationMap.values(), o -> MapUtils.Pair.T(o.getName(), o)));
    this.subjectMap = ObjectUtils.ifNotNull(subjectMap, Collections::unmodifiableMap, Collections.emptyMap());
  }

  /**
   * Returns a Function or FunctionGroup identified by its name.
   * <p>
   * It will return NULL if Function or FunctionGroup is not defined.
   *
   * @param name Name of Function or FunctionGroup
   * @return Function or FunctionGroup identified by name
   */
  public Function getFunction(String name) {
    return functionMap.get(name);
  }

  /**
   * Returns an Organization or OrganizationGroup identified by its internal ID.
   * <p>
   * It will return NULL if Organization or OrganizationGroup is not defined.
   *
   * @param internalID Internal ID of Organization or OrganizationGroup
   * @return Organization or OrganizationGroup identified by internal ID
   */
  public Organization getOrganization(long internalID) {
    return organizationMap.get(internalID);
  }

  /**
   * Returns an Organization or OrganizationGroup identified by its name.
   * <p>
   * It will return NULL if Organization or OrganizationGroup is not defined.
   *
   * @param name Name of Organization or OrganizationGroup
   * @return Organization or OrganizationGroup identified by name
   */
  public Organization getOrganization(String name) {
    return organizationByNameMap.get(name);
  }

  /**
   * Returns a Subject or SubjectGroup identified by its internal ID.
   * <p>
   * It will return NULL if Subject or SubjectGroup is not defined.
   *
   * @param internalID Internal ID of Subject or SubjectGroup
   * @return Subject or SubjectGroup identified by internal ID
   */
  public Subject getSubject(long internalID) {
    return subjectMap.get(internalID);
  }

  /**
   * Returns the parent OrganizationGroups of an Organization identified by the Organization's internal ID.
   * <p>
   * It will return an empty set if the Organization has no parent.
   *
   * @param internalID Internal ID of Organization or OrganizationGroup
   * @return Parent OrganizationGroups
   */
  public Set<Organization> getParentOrganizations(long internalID) {
    // Resolve the direct parents of the organization identified by 'internalID'.
    Set<Organization> parents = organizationMap.values().stream()
            .filter(Organization::isGroup)
            .map(OrganizationGroup.class::cast)
            .filter(o -> o.getMembers().contains(internalID))
            .collect(Collectors.toSet());

    // Also resolve the parents of parents recursively.
    for (Organization parent : SetUtils.set(parents)) {
      parents.addAll(getParentOrganizations(parent.getInternalID()));
    }

    return parents;
  }

  /**
   * Returns the children of an Organization identified by its internal ID.
   * <p>
   * It will return an empty set if the Organization has no children.
   *
   * @param internalID Internal ID of Organization or OrganizationGroup
   * @return Child Organizations
   */
  public Set<Organization> getChildOrganizations(long internalID) {
    Set<Organization> children = SetUtils.set();

    // Fetch organization identified by 'internalID'.
    Organization organization = organizationMap.get(internalID);
    if (organization == null || !organization.isGroup()) {
      // No group, no children.
      return children;
    }

    // Resolve all direct children and recursively their children.
    for (Long id : OrganizationGroup.class.cast(organization).getMembers()) {
      Organization child = organizationMap.get(id);
      if (child != null) {
        children.add(child);
        children.addAll(getChildOrganizations(child.getInternalID()));
      }
    }

    return children;
  }

  /**
   * Returns the parent SubjectGroups of a Subject identified by the Subjects's internal ID.
   * <p>
   * It will return an empty set if the Subject has no parent.
   *
   * @param internalID Internal ID of Subject or SubjectGroup
   * @return Parent SubjectGroups
   */
  public Set<Subject> getParentSubjects(long internalID) {
    // Resolve the direct parents of the subject identified by 'internalID'.
    Set<Subject> parents = subjectMap.values().stream()
            .filter(Subject::isGroup)
            .map(SubjectGroup.class::cast)
            .filter(o -> o.getMembers().contains(internalID))
            .collect(Collectors.toSet());

    // Also resolve the parents of parents recursively.
    for (Subject parent : SetUtils.set(parents)) {
      parents.addAll(getParentSubjects(parent.getInternalID()));
    }

    return parents;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private Map<String, Function> functionMap;
    private Map<Long, Organization> organizationMap;
    private Map<Long, Subject> subjectMap;

    private Builder() {
    }

    public AccessControllerState build() {
      return new AccessControllerState(functionMap, organizationMap, subjectMap);
    }

    public Builder setFunctions(Set<Function> functions) {
      this.functionMap = MapUtils.map(functions, f -> MapUtils.Pair.T(f.getName(), f));
      return this;
    }

    public Builder addFunction(Function function) {
      this.functionMap = MapUtils.addToMap(this.functionMap, function.getName(), function);
      return this;
    }

    public Builder setOrganizations(Set<Organization> organizations) {
      this.organizationMap = MapUtils.map(organizations, o -> MapUtils.Pair.T(o.getInternalID(), o));
      return this;
    }

    public Builder addOrganization(Organization organization) {
      this.organizationMap = MapUtils.addToMap(this.organizationMap, organization.getInternalID(), organization);
      return this;
    }

    public Builder setSubjects(Set<Subject> subjects) {
      this.subjectMap = MapUtils.map(subjects, s -> MapUtils.Pair.T(s.getInternalID(), s));
      return this;
    }

    public Builder addSubject(Subject subject) {
      this.subjectMap = MapUtils.addToMap(this.subjectMap, subject.getInternalID(), subject);
      return this;
    }
  }
}
