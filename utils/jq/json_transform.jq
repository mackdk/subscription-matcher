# Path-based anonymization transform.
# jq vars:
#   $subIdMap: old subscription ID (string) -> new ID (number)
#   $sccMap: old SCC username (string) -> new username (string)
#   $systemMap: old system name (string) -> new system name (string)
#   $useSystemName: true to rename system names, false to remove them

# Remap subscription_id in match-like objects.
def remap_match:
    if has("subscription_id") and (.subscription_id | type == "number") and ($subIdMap[(.subscription_id | tostring)] != null) then
        .subscription_id = $subIdMap[(.subscription_id | tostring)]
    end;

# Remap subscription object fields:
# - id in subscriptions[]
# - scc_username in subscriptions[]
def remap_subscription:
    if has("id") and (.id | type == "number") and ($subIdMap[(.id | tostring)] != null) then
        .id = $subIdMap[(.id | tostring)]
    end
    | if has("scc_username") and (.scc_username | type == "string") and ($sccMap[.scc_username] != null) then 
        .scc_username = $sccMap[.scc_username]
    end;

# Remap or remove system name based on $useSystemName.
def remap_system_name:
    if has("name") and (.name | type == "string") then
        if $useSystemName and ($systemMap[.name] != null) then
            .name = $systemMap[.name]
        else
            del(.name)
        end
    end;

# Remap keys of subscription_policies object (keys are subscription IDs).
def remap_subscription_policies:
    with_entries(
        .key = (($subIdMap[.key] // .key) | tostring)
    );

# Remap subscription ids inside messages[].data, if they exist.
def remap_messages:
    if has("data") and (.data | type == "object") then
        .data |= (
            if has("new_subscription_id") and (.new_subscription_id | type == "string") and ($subIdMap[.new_subscription_id] != null) then
                .new_subscription_id = (($subIdMap[.new_subscription_id]) | tostring)
            end
            | if has("old_subscription_id") and (.old_subscription_id | type == "string") and ($subIdMap[.old_subscription_id] != null) then
                .old_subscription_id = (($subIdMap[.old_subscription_id]) | tostring)
              end
            | if has("subscription_id") and (.subscription_id | type == "string") and ($subIdMap[.subscription_id] != null) then
                .subscription_id = (($subIdMap[.subscription_id]) | tostring)
              end
        )
    end;


# Recursive walker to navigate through children, then apply path-specific remaps.
def walk_all:
    if type == "object" then
        with_entries(.value |= walk_all)
        # output.json: matches[].subscription_id
        | if has("matches") and (.matches | type == "array") then
            .matches |= map(remap_match)
          end
        # input.json: pinned_matches[].subscription_id
        | if has("pinned_matches") and (.pinned_matches | type == "array") then
            .pinned_matches |= map(remap_match)
          end
        # input/output: subscriptions[].id + subscriptions[].scc_username
        | if has("subscriptions") and (.subscriptions | type == "array") then
            .subscriptions |= map(remap_subscription)
          end
        # input/output: systems[].name
        | if has("systems") and (.systems | type == "array") then
            .systems |= map(remap_system_name)
          end
        # input: virtualization_groups[].name
        | if has("virtualization_groups") and (.virtualization_groups | type == "array") then
            .virtualization_groups |= map(remap_system_name)
          end
        # output.json: subscription_policies
        | if has("subscription_policies") and (.subscription_policies | type == "object") then
            .subscription_policies |= remap_subscription_policies
          end
        # output.json: messages[]
        | if has("messages") and (.messages | type == "array") then
            .messages |= map(remap_messages)
          end
    end;

# Entry point.
walk_all
