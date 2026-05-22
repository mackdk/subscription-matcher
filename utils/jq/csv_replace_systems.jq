def rx_escape:
    gsub("([.^$|()\\[\\]{}*+?\\\\-])"; "\\\\\\1");

split("\n")
| map(select(length > 0))
| ($systemMap | to_entries) as $entries
| map(reduce $entries[] as $e (.;
    gsub("," + ($e.key | rx_escape) + ","; "," + $e.value + ",")
))
| .[]
