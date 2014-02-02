
var App = function() {

    var object_info = {};

    function safe_tags(str) {
        return str.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;') ;
    }

    function validIsbn10(isbn) {
       var n, i, sum = 0;
       isbn = isbn.toString(); // make sure it's a string
       if (isbn.length !== 10) return false;
       for (i=0; i < 10; i++) {
         n = (isbn[i] === 'X') ? 10 : parseInt(isbn[i]);
         sum += n * (10-i);
       }
       return sum % 11 === 0;
    }

    function validIsbn13(isbn) {
        var check, i;
        isbn = isbn.toString(); // make sure it's a string
        if (isbn.length !== 13) return false;
        check = 0;
        for (i = 0; i < 13; i += 2) {
          check += parseInt(isbn[i]);
        }
        for (i = 1; i < 12; i += 2){
          check += 3 * parseInt(isbn[i]);
        }
        return check % 10 === 0;
    }

    function sruItemInfoSuccess(response) {
        var isbn = '',
            isbns = [];

        object_info.isbn = response.isbn ? response.isbn : [];
        object_info.authors = response.authors ? response.authors : [];
        object_info.title = response.title ? response.title : '';
        object_info.subtitle = response.subtitle ? response.subtitle : '';
        object_info.year = response.year ? response.year : '';
        object_info.pages = response.pages ? response.pages : '';
        object_info.added_author = response.added_author ? response.added_author : '';
        object_info.classifications = response.classifications ? response.classifications : [];
        object_info.subjects = response.subjects ? response.subjects : [];

        if (response.isbn) {
            $.getJSON('http://services.biblionaut.net/content.php?isbn=' + object_info.isbn[0])
                .success(contentInfoSuccess)
                .error(error);
        } else {
            makeView(object_info);
        }
    }

    function contentInfoSuccess(response) {
        if (response.thumb) object_info.thumb = response.thumb;
        if (response.short_desc) object_info.short_desc = response.short_desc;
        if (response.long_desc) object_info.long_desc = response.long_desc;

        makeView(object_info);
    }

    function error(data) {
        $('#msg').append('Oppslaget gikk ut i feil');
    }

    function contentSuccess(data) {
        if (data.thumb !== undefined) object_info.thumb = data.thumb;
        if (data.short_desc !== undefined) object_info.short_desc = data.short_desc;
        if (data.long_desc !== undefined) object_info.long_desc = data.long_desc;
        if (data.toc !== undefined) object_info.toc = data.toc;
        makeView(object_info);
    }

    function makeView(book) {
        console.log(book);
        $('#msg').append('<hr />');
        if (book.thumb) {
            $('#msg').append('<img src="' + book.thumb + '" style="float:right; max-width: 25%;">');
        }
        $('#msg').append('<a href="http://ask.bibsys.no/ask/action/show?pid=' + book.objektid + '&kid=biblio"><strong>' + book.title + ' ' + book.subtitle + '</strong></a> (' + book.year + ')<br />');

        $.each(book.authors, function(k, author) {
            var aname = author.name;

            if (author.authority) {
                var autid = author.authority.substr(author.authority.indexOf(')') + 1);
                var aname = '<a href="http://ask.bibsys.no/ask/action/result?cmd=&kilde=biblio&cql=bs.autid+%3D+' + autid + '">' + aname + '</a>';
                // TODO: Prøve å hente VIAF-id fra http://data.bibsys.no/data/notrbib/authorityentry/<autid>
            }
            $('#msg').append(aname + '<br />');
        });

        $('#msg').append(book.pages + '<br />');

        if (book.isbn) {
            var url = 'http://no.wikipedia.org/w/index.php?title=Spesial%3ABokkilder&isbn=' + book.isbn;
            $('#msg').append('ISBN: <a href="' + url + '">' + book.isbn + '</a><br />');
        }

        if (book.classifications !== undefined) {
            var klasses = [];
            for (var i = 0; i < book.classifications.length; i++) {
                klasses.push(book.classifications[i].number + ' <small>('+book.classifications[i].system + ')</small>'); 
            }
            $('#msg').append('Klass: ' + klasses.join(', ') + '<br />');
        }

        if (book.subjects !== undefined) {
            var subjects = [];
            for (var i = 0; i < book.subjects.length; i++) {
                subjects.push(book.subjects[i].term + ' <small>('+book.subjects[i].system + ')</small>'); 
            }
            $('#msg').append('Emner: ' + subjects.join(', ') + '<br />');
        }
        if (book.short_desc !== undefined) {
            $('#msg').append('<hr /><p>' + book.short_desc + '</p>');
        }
        if (book.long_desc !== undefined) {
            $('#msg').append('<hr /><p>' + book.long_desc + '</p>');
        }
    }

    this.barcodeScanned = function(strekkode) {

        object_info = {};
        object_info.barcode = strekkode

        $('#msg').html('Henter objektid for ' + object_info.barcode + '...<br />');
        $.getJSON('http://services.biblionaut.net/getids.php?id=' + object_info.barcode, function(response) {
            object_info.objektid = response.objektid

            $('#msg').append('Henter metadata for objekt ' + object_info.objektid + '...<br />');
            if (object_info.objektid.length === 9) {
                $.getJSON('http://services.biblionaut.net/sru_iteminfo.php?objektid=' + object_info.objektid)
                    .success(sruItemInfoSuccess)
                    .error(error);
            }
        });
    };

};

window.app = new App();

$(document).ready(function() {

    function querystring(key) {
       var re = new RegExp('(?:\\?|&)'+key+'=(.*?)(?=&|$)','gi');
       var r = [], m;
       while ((m = re.exec(document.location.search)) != null) r.push(m[1]);
       return r;
    }

    var bc = querystring('strekkode');
    if (bc.length == 1) {
        window.app.barcodeScanned(bc[0]);
    }
});

// Global function available to the app
function barcodeScanned(barcode) {
    window.app.barcodeScanned(barcode);
}
