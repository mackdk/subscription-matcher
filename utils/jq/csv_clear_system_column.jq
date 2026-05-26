split("\n")
| map(select(length > 0))
| to_entries
| map(
    if .key == 0 then
        .value
    else
        (.value
        | if test("^[^,]*,[^,]*,") then
              capture("^(?<first>[^,]*),[^,]*,(?<rest>.*)$")
              | "\(.first),,\(.rest)"
          else
              .
          end)
    end
)
| .[]
