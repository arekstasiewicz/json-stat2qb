package org.deri.jsonstat2qb.jsonstat.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.util.ISO8601DateFormat;

import net.hamnaberg.funclite.CollectionOps;
import net.hamnaberg.funclite.Optional;

import org.deri.jsonstat2qb.jsonstat.*;
import org.joda.time.DateTime;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;

public class JacksonStatParser {
    private ObjectMapper mapper;

    public JacksonStatParser(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public JacksonStatParser() {
        this(new ObjectMapper());
    }

    public Stat parse(InputStream stream) throws IOException {
        try(InputStream is = stream) {
            JsonNode tree = mapper.readTree(is);
            return parse((ObjectNode)tree);
        }
    }

    private Stat parse(ObjectNode tree) {
        Iterator<Map.Entry<String,JsonNode>> fields = tree.fields();
        List<Dataset> datasets = new ArrayList<>();

        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> next = fields.next();
            datasets.add(parseDataset(next));
        }
        return new Stat(datasets);
    }

    private Dataset parseDataset(Map.Entry<String, JsonNode> entry) {
        JsonNode node = entry.getValue();
        Optional<String> label = Optional.none();
        Optional<DateTime> updated = Optional.none();
        List<Data> values = CollectionOps.newArrayList();
        if (node.has("label")) {
            label = Optional.fromNullable(node.get("label").asText());
        }
        if (node.hasNonNull("updated")) {
        	// check date format to avoid errors - expect ISO 8601
        	ISO8601DateFormat df = new ISO8601DateFormat();
        	String dateset_updated = node.get("updated").asText();

        	try {
				Date d = df.parse(dateset_updated);
				updated = Optional.some(new DateTime(d));
				
			} catch (ParseException e) {
				// wrong format - leave empty
			} catch (IllegalArgumentException e) {
				// wrong format - leave empty

				// TEMP - one more try - dd/MM/yyyy hh:mm:ss aa
				String cso_format = "dd/MM/yyyy HH:mm:ss aa";
				DateFormat cso_df = new SimpleDateFormat(
						cso_format, Locale.ENGLISH);

				Date result = null;
				
				try {
					result = cso_df.parse(dateset_updated);
					updated = Optional.some(new DateTime(result));
				} catch (ParseException e1) {
					// wrong format - leave empty
				}				
			}

		}

        if (node.hasNonNull("value")) {
            for(JsonNode v : node.get("value")) {
                Object value;
                if (v.isNumber()) {
                    value = v.decimalValue();
                } else {
                    value = v.asText();
                }
                if (value != null) {
                    values.add(new Data(value, Optional.<String>none())); //Handle status...
                }
            }
        }

        Map<String, Dimension> dimensions = new LinkedHashMap<>();

        if (node.hasNonNull("dimension")) {
            JsonNode dims = node.get("dimension");
            JsonNode ids = dims.get("id");
            JsonNode sizes = dims.get("size");
            
            HashMap<String, String> dimensionsRole = new HashMap<String, String>();

            // build HashMap for dimensions
        	if (dims.hasNonNull("role")) {
        		JsonNode roles = dims.get("role");

        		Iterator<Entry<String, JsonNode>> fields = roles.fields();

        	     while(fields.hasNext()) {

        	    	 Map.Entry<?,?> element = fields.next();
                     ArrayNode roleDimensions = (ArrayNode) element.getValue();
                     Iterator<JsonNode> roleDimensionsIterator = roleDimensions.iterator();

                     while (roleDimensionsIterator.hasNext()) {
                         JsonNode tnode  = roleDimensionsIterator.next();
                         dimensionsRole.put(tnode.asText(), (String) element.getKey() );
                     }

        	      }
        	}
            
            
            for (int i = 0; i < ids.size(); i++) {
                String id = ids.get(i).asText();
                int currentSize = sizes.get(i).intValue();
                if (dims.hasNonNull(id)) {
                    JsonNode dimension = dims.get(id);
                    
                    ObjectNode tmpDimension = (ObjectNode) dimension;
                    
                    if (dimensionsRole.containsKey(id)){
                    	tmpDimension.put("role", dimensionsRole.get(id));
                    }

                    dimensions.put(id, parseDimension(i, id, currentSize, (JsonNode) tmpDimension));
                }
            }
        }

        return new Dataset(entry.getKey(), label, values, updated, dimensions);
    }

    private Dimension parseDimension(int index, String id, int currentSize, JsonNode dimension) {
        Optional<String> label = Optional.none();

        if (dimension.has("label")) {
            label = Optional.fromNullable(dimension.get("label").asText());
        }

        JsonNode category = dimension.get("category");

        Optional<Role> dimRole;

        try {
        	dimRole = Optional.some( Role.valueOf( dimension.get("role").asText() ));
		} catch (NullPointerException e) {
			dimRole = Optional.<Role>none();
		}
        
        return new Dimension(index, id, currentSize, label, parseCategory(category), dimRole); //handle roles
    }

    private Category parseCategory(JsonNode category) {
        Map<String, Integer> indices = new LinkedHashMap<>();
        Map<String, String> labels = new LinkedHashMap<>();
        Map<String, List<String>> children = new LinkedHashMap<>();
        if (category != null) {
            if (category.has("index")) {
                JsonNode index = category.get("index");
                if (index.isArray()) {
                    int i = 0;
                    for (JsonNode id : index) {
                        indices.put(id.asText(), i);
                        i++;
                    }
                }
                Iterator<Map.Entry<String, JsonNode>> fields = index.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> entry = fields.next();
                    indices.put(entry.getKey(), entry.getValue().intValue());
                }
            }
            if (category.has("label")) {
                JsonNode label = category.get("label");
                Iterator<Map.Entry<String, JsonNode>> fields = label.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> entry = fields.next();
                    labels.put(entry.getKey(), entry.getValue().asText());
                }
            }
            if (category.has("child")) {
                JsonNode child = category.get("child");
                Iterator<Map.Entry<String, JsonNode>> fields = child.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> entry = fields.next();
                    List<String> c = new ArrayList<>();
                    for (JsonNode node : entry.getValue()) {
                        c.add(node.asText());
                    }
                    children.put(entry.getKey(), c);
                }
            }
        }
        return new Category(indices, labels, children);
    }
}
