var express = require('express');
var app = express();


/** SQL Query **/
var mysql = require("mysql");
var pagination = require('pagination');
var bodyParser = require("body-parser");
app.use(bodyParser.urlencoded({extented: false}));

/**
 localhost setting:
 host: "localhost",
 user:"root",
 password:"123456",
 database:"cs223"
 **/

var db_config = {
    host: "108.179.232.69",
    user: "jeiqichu_roy",
    password: "be223db",
    database: "jeiqichu_test"
}
var con;

function db_connection() {
    con = mysql.createConnection(db_config);

    con.connect(function (err) {
        if (err) {
            console.log("Error connecting to database");
            setTimeout(db_connection, 2000);
        }
        console.log("Connection opened!");
    })

    con.on('error', function (err) {
        console.log('db error', err);
        if (err.code === 'PROTOCOL_CONNECTION_LOST') {
            db_connection();
        } else {
            throw err;
        }
    });
}

function get_color(i){
    colors = ['#29339B','#CAA8F5','#ED0E0E','#BCF4F5','#FAE8EB'];
    return colors[i%colors.length];
}

function google_chart(in_json, number) {
    var count = 0;
    var out_str = '';
    out_str = '{';
    out_str += '"cols":[{"id": "", "label": "Name", "pattern":"", "type": "string"},{"id": "", "label": "Percentage/Hits", "pattern":"", "type": "number"}, {"id":"","role":"style","type":"string"}],';
    out_str += '"rows":['
    var keys = Object.keys(in_json.terms);
    keys.forEach(function (k) {
        if (count < number){
            var color = get_color(count);
            out_str += '{"c":[{"v":"' + k + '",},{"v":' + in_json.terms[k] + '},{"v":"'+color+'"}]},'
        }
        count++;
    })
    out_str += '			]'
    out_str += '}'
    return out_str;
}

//Google Charts get file path by which_data
function gc_get_file(which_data){
	switch (which_data){
		case "1": 
			return "public/mm_type_precentage.json";
		case "2":
			return "public/mm_type_frequency.json";
		case "3":
			return "public/mm_type_precentageCL_Age_Restriction.json"
		case "4":
			return "public/mm_type_precentageCL_Annotation.json";
		case "5":			
			return "public/mm_type_precentageCL_Current_Restriction.json";
		case "6":
			return "public/mm_type_precentageCL_Health_Condition_Restriction.json";
		case "7":
			return "public/mm_type_precentageCL_MaleFemale_Restriction.json"
		case "8":
			return "public/mm_type_precentageCL_PriorRestriction.json";
		case "9":
			return "public/mm_type_precentageCL_Time_Restriction.json";
	}
	return null;
}

function gc_get_title(which_data){
	switch (which_data){
		case "1":
			return "Semantic Type Precentage";
		case "2":
			return "Term Hits";
		case "3":
			return "Age Restriction";
		case "4":
			return "Annotation";
		case "5":
			return "Current Restriction";
		case "6":
			return "Health Condition Restriction"
		case "7":
			return "MaleFemale Restriction";
		case "8":
			return  "PriorRestriction";
		case "9":
			return  "Time Restriction";
	}
	return null;
}

function gc_res_render(which_data,number,response, page){
	var title = gc_get_title(String(which_data));	
	page = page || "pages/meta_map";
	response.render(page,{
		'which_data':which_data,
	  	'number':number,
	  	'title': title});
}

function get_bgstyle(i){
    var bgs = ["bg-primary", "bg-success", "bg-info", "bg-warning","bg-danger"];
    return bgs[i%bgs.length];
}

function interpre_anno(abbr){
    var inter_dict = {"CC":"Coordinating conjunction",
    "CD":"Cardinal number",
    "DT":"Determiner",
    "EX":"Existential there",
    "FW":"Foreign word",
    "IN":"Preposition or subordinating conjunction",
    "JJ":"Adjective",
    "JJ":"Adjective, comparative",
    "JJ":"Adjective, superlative",
    "LS":"List item marker",
    "MD":"Modal",
    "NN":"Noun, singular or mass",
    "NN":"Noun, plural",
    "NN":"Proper noun, singular",
    "NNP":"Proper noun, singular",
    "NNPs":    "Proper noun, plural",
    "PD":"Predeterminer",
    "PO":"Possessive ending",
    "PR":"Personal pronoun",
    "PRP$":    "Possessive pronoun",
    "RB":"Adverb",
    "RB":"Adverb, comparative",
    "RB":"Adverb, superlative",
    "RP":"Particle",
    "SY":"Symbol",
    "TO":"to",
    "UH":"Interjection",
    "VB":"Verb, base form",
    "VB":"Verb, past tense",
    "VB":"Verb, gerund or present participle",
    "VB":"Verb, past participle",
    "VB":"Verb, non-3rd person singular present",
    "VB":"Verb, 3rd person singular present",
    "WD":"Wh-determiner",
    "WP":"Wh-pronoun",
    "WP":"Possessive wh-pronoun",
    "WR":"Wh-adverb"}
    if (inter_dict[abbr]!=undefined)
        return inter_dict[abbr];
    return abbr;
}

function mk_annotCriteria(rows){
    dict = {};
    out_str = "";
    for (i = 0 ; i < rows.length; i++){
        r = rows[i];
        f = JSON.parse(r.features);
        if (f.category != undefined){
            if (f.category.length > 1){
                key = interpre_anno(f.category);
                if (!(key in dict)){
                    dict[key] = get_bgstyle(Object.keys(dict).length);
                }
                out_str += "<a href='#'' data-toggle='tooltip' title='"+key+"'><span class='"+dict[key]+" text-muted'>"+r.value+"</span></a> ";
            }
            else{
                out_str += r.value + " ";       
            }
        }
    }
    return [dict, out_str];
}

db_connection();

app.set('port', (process.env.PORT || 5000));

app.use(express.static(__dirname + '/public'));

// views is directory for all template files
app.set('views', __dirname + '/views');
app.set('view engine', 'ejs');

app.get('/', function (request, response) {
    response.render('pages/index');
});

app.get("/demo", function (request, response) {
    // Load data only once
    con.query('SELECT * FROM med_info LIMIT 20;', function (err, rows) {
        response.render('pages/demo', {
            'results': rows
        });
        data_loaded = true;
    });
});

var rowsPerPage = 20;
var currentPage = 1;
var totalResult = 6653;

function createPaginator(prelink, currentPage,  totalResult) {
    var boostrapPaginator = new pagination.TemplatePaginator({
        prelink: prelink, current: currentPage, rowsPerPage: rowsPerPage,
        totalResult: totalResult, slashSeparator: true,
        template: function (result) {
            var i, len, prelink;
            var html = '<div><ul class="pagination .pagination-lg">';
            if (result.pageCount < 2) {
                html += '</ul></div>';
                return html;
            }
            prelink = this.preparePreLink(result.prelink);
            if (result.previous) {
                html += '<li><a href="' + prelink + result.previous + '">' + this.options.translator('PREVIOUS') + '</a></li>';
            }
            if (result.range.length) {
                for (i = 0, len = result.range.length; i < len; i++) {
                    if (result.range[i] === result.current) {
                        html += '<li class="active"><a href="' + prelink + result.range[i] + '">' + result.range[i] + '</a></li>';
                    } else {
                        html += '<li><a href="' + prelink + result.range[i] + '">' + result.range[i] + '</a></li>';
                    }
                }
            }
            if (result.next) {
                html += '<li><a href="' + prelink + result.next + '" class="paginator-next">' + this.options.translator('NEXT') + '</a></li>';
            }
            html += '</ul></div>';
            return html;
        }
    });
    return boostrapPaginator;
}

app.get("/get_json/w/:which_data/n/:number", function(request,response){
	var jsonfile = require('jsonfile')
	var which_data = request.params.which_data;
	var file = gc_get_file(which_data);
	var number = request.params.number;
	jsonfile.readFile(file, function(err, obj) {
	  response.send(google_chart(obj,number));
	})
})

app.get("/msa_charts/", function(request, response){
	var which_data = 3;
	var number = 20;
	var title = gc_get_title(which_data);
	gc_res_render(which_data,number,response,"pages/msa_charts");	
})


app.get("/msa_charts/w/:which_data/n/:number", function(request, response){
	var which_data = request.params.which_data;
	var number = request.params.number;
	var title = gc_get_title(which_data);
	gc_res_render(which_data,number,response, "pages/msa_charts");	
})

// Metamap Visuals
app.get("/meta_map/",function(request,response){
	var which_data = 1;
	var number = 20;
	var title = gc_get_title(which_data);
	gc_res_render(which_data,number,response);
})

app.get("/meta_map/w/:which_data/n/:number",function(request,response,next){
	var which_data = request.params.which_data;
	var number = request.params.number;
	var title = gc_get_title(which_data);
	gc_res_render(which_data,number,response);
})

app.get("/trail", function (request, response) {
    boostrapPaginator = createPaginator('/trail',1, totalResult)
    con.query('SELECT * FROM med_info LIMIT 0,' + rowsPerPage + ";", function (err, rows) {
        response.render('pages/trail', {
            'results': rows,
            'keywords': "",
            'nav_render': boostrapPaginator.render()
        });
    });
})

app.post("/trail", function(request, response){
    var search = request.body.search;
    var pageNum = 1;
    boostrapPaginator = createPaginator("/trail/c/"+search,pageNum, totalResult)
    con.query('SELECT * FROM med_info where criteria LIKE \'% '+search+' %\' or brief_title LIKE \'% '+search+' %\' LIMIT ' + (rowsPerPage * (pageNum - 1)) + ',' + rowsPerPage + ";", function (err, rows) {       
        response.render('pages/trail', {
            'results': rows,
            'keywords': search,
            'nav_render': boostrapPaginator.render()
        });
    });    
})


app.get("/trail/page/:pageNum", function (request, response, next) {
    pageNum = request.params.pageNum;
    boostrapPaginator = createPaginator("/trail",pageNum, totalResult)
    con.query('SELECT * FROM med_info LIMIT ' + (rowsPerPage * (pageNum - 1)) + ',' + rowsPerPage + ";", function (err, rows) {
        response.render('pages/trail', {
            'results': rows,
            'keywords': "",
            'nav_render': boostrapPaginator.render()
        });
    });
})

app.get("/trail/c/:search/page/:pageNum", function (request, response, next) {
    pageNum = request.params.pageNum;
    search = request.params.search;
    boostrapPaginator = createPaginator("/trail/c/"+search,pageNum, totalResult)
    con.query('SELECT * FROM med_info where criteria LIKE \'% '+search+' %\' or brief_title LIKE \'% '+search+' %\' LIMIT ' + (rowsPerPage * (pageNum - 1)) + ',' + rowsPerPage + ";", function (err, rows) {
        response.render('pages/trail', {
            'results': rows,
            'keywords': search,
            'nav_render': boostrapPaginator.render()
        });
    });

})

app.get("/trail/id/:id", function (request, response, next) {
    id = request.params.id;
    con.query('SELECT * FROM med_info where id = ' + id + ";", function (err, doc_rows) {
        con.query('SELECT * FROM annotation where document_id ='+id+';',function(err2, anno_rows){
            res = mk_annotCriteria(anno_rows);           
            response.render('pages/trail_single', {
                'r': doc_rows[0],
                'cat_dict': res[0],
                'out_str': res[1]
            });
        })
    });
})

app.get("/lan_var", function(request,response){
    response.render('pages/lan_var');
})

app.get("/chart", function (request, response) {
    // Load data only once
    con.query('select * from googlechart_test', function (err, rows) {
        console.log("rows=" + rows)

        var dataAll = [];
        // for (var i = 0; i < rows.size; i++) {
        //     var dataCell = [];
        //     dataCell[0] = rows[i].x;
        //     dataCell[1] = rows[i].y;
        //     dataAll[i] = dataCell;
        // }
        rows.forEach(function (r) {
            var dataCell = [];
            dataCell[0] = r.x;
            dataCell[1] = r.y;
            dataAll.push(dataCell);
        });

        data = '{"name":"jim","age":23}';

        response.render('pages/chart', {
            'results': rows,
            'dataAll': dataAll
        });
        data_loaded = true;
    });
});


app.get("/chart1", function (request, response) {
    response.render('pages/chart1', {});
    data_loaded = true;
});

app.get("/chart2", function (request, response) {
    response.render('pages/chart2', {});
    data_loaded = true;
});

app.get("/chart3", function (request, response) {
    response.render('pages/chart3', {});
    data_loaded = true;
});

app.get("/chart4", function (request, response) {
    con.query('select profile,rows from msa_profile', function (err, rows) {
        var data_map = {};
        rows.forEach(function (r) {
            var profiles = r.profile.split(',');
            var x = profiles.length + "";
            var y = r.rows;
            if (data_map.hasOwnProperty(x)) {
                data_map[x] += y;
            } else {
                data_map[x] = y;
            }
        });

        var xs = [];
        var ys = [];
        for (var prop in data_map) {
            if (data_map.hasOwnProperty(prop)) {
                // console.log('key is ' + prop +' and value is' + data_map[prop]);
                xs.push(prop);
                ys.push(data_map[prop]);
            }
        }

        response.render('pages/chart4', {
            'xs': xs,
            'ys': ys
        });
        data_loaded = true;
    });
});

app.get("/chart5", function (request, response) {
    con.query('select profile,rows from msa_profile', function (err, rows) {
        var data_map = {};
        rows.forEach(function (r) {
            var profiles = r.profile.split(',');
            var x = profiles.length + "";
            var y = r.rows;
            if (data_map.hasOwnProperty(x)) {
                data_map[x] += y;
            } else {
                data_map[x] = y;
            }
        });

        var xs = [];
        var ys = [];
        for (var prop in data_map) {
            if (data_map.hasOwnProperty(prop)) {
                // console.log('key is ' + prop +' and value is' + data_map[prop]);
                xs.push(prop);
                ys.push(data_map[prop]);
            }
        }

        response.render('pages/chart5', {
            'xs': xs,
            'ys': ys
        });
        data_loaded = true;
    });
});

app.post("/chart5", function (request, response) {
    var search = request.body.search;

    con.query('select profile,rows from msa_profile', function (err, rows) {
        var data_map = {};
        rows.forEach(function (r) {
            var profiles = r.profile.split(',');
            var x = profiles.length + "";
            var y = r.rows;
            if (r.profile.indexOf(search) >= 0) {
                if (data_map.hasOwnProperty(x)) {
                    data_map[x] += y;
                } else {
                    data_map[x] = y;
                }
            }
        });

        var xs = [];
        var ys = [];
        for (var prop in data_map) {
            if (data_map.hasOwnProperty(prop)) {
                // console.log('key is ' + prop +' and value is' + data_map[prop]);
                xs.push(prop);
                ys.push(data_map[prop]);
            }
        }

        response.render('pages/chart5', {
            'xs': xs,
            'ys': ys
        });
        data_loaded = true;
    });
});


app.listen(app.get('port'), function () {
    console.log('Node app is running on port', app.get('port'));
});


