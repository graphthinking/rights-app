Handlebars.registerHelper('json', function (obj, options) {
  return new Handlebars.SafeString(JSON.stringify(obj, null, 2));
});

Handlebars.registerHelper('a', function (href, options) {
  return new Handlebars.SafeString("<a href=\"" + href + "\">" + href + "</a>");
});

Handlebars.registerHelper('resource', function(id, graph, options) {
if(!!graph) {
  for (var i = 0; i < graph.length; i++) {
    if(graph[i]['@id']==id){
      return options.fn(graph[i]);
    }
  }
}
return "undefined graph";

});

Handlebars.registerHelper('property', function(property, graph, options) {

  var sort = options.hash['sort'].split(" ");
  var on_property = sort[0];
  var order = sort[1];

  var graphs = [];

  for (var i = 0; i < property.length; i++) {
    for (var j = 0; j < graph.length; j++) {
        if(graph[j]['@id']==property[i]) {
        graphs.push(graph[j]);
      }
    }
  }

  var ret = "";

  graphs.sort(order == "asc" ? sort_property_asc(on_property) : sort_property_desc(on_property));

  for (var k = 0; k < graphs.length; k++) {
    ret = ret + options.fn(graphs[k]);
  }

  return ret;

});

Handlebars.registerHelper('sort', function(values, options) {
  // weird Rhino behavior, explicitly create array
  var vals = [];
  for (var i = 0; i < values.length; i++) {
    vals.push(values[i]);
  }
  vals.sort(options.hash['direction'] == "asc" ? sort_asc : sort_desc);
  var ret = "";
  for (var i = 0; i < vals.length; i++) {
    ret = ret + options.fn(vals[i]);
  }
  return ret;
});

function sort_property_asc(on_property) {
  return function(a, b) {
    return a[on_property]['@value'] == b[on_property]['@value']
        ? 0 : +(a[on_property]['@value'] > b[on_property]['@value']) || -1;
  }
}

function sort_property_desc(on_property) {
  return function(a, b) {
    return a[on_property]['@value'] == b[on_property]['@value']
        ? 0 : +(a[on_property]['@value'] < b[on_property]['@value']) || -1;
  }
}

function sort_asc(a, b) {
  return a['@value'] == b['@value']
      ? 0 : +(a['@value'] > b['@value']) || -1;
}

function sort_desc(a, b) {
  return a['@value'] == b['@value']
      ? 0 : +(a['@value'] < b['@value']) || -1;
}
