
(function() {

    var object_info = {};

    function querystring(key) {
       var re=new RegExp('(?:\\?|&)'+key+'=(.*?)(?=&|$)','gi');
       var r=[], m;
       while ((m=re.exec(document.location.search)) != null) r.push(m[1]);
       return r;
    }

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

    function sruItemInfoSuccess(book) {

        if (book.isbn !== undefined) {
            var isbns = [];
            for (var i = 0; i < book.isbn.length; i++) {
                if (validIsbn13(book.isbn[i])) isbn13 = book.isbn[i];
                if (validIsbn10(book.isbn[i])) isbn10 = book.isbn[i];
            }
        }
        // Prefer isbn13 over isbn10:
        if (isbn13 !== undefined) isbn = isbn13;
        else if (isbn10 !== undefined) isbn = isbn10;

        object_info.isbn = isbn;
        object_info.title = book.title ? book.title : '';
        object_info.subtitle = book.subtitle ? book.subtitle : '';
        object_info.year = book.year ? book.year : '';
        object_info.pages = book.pages ? book.pages : '';
        object_info.main_author = book.main_author ? book.main_author : '';
        object_info.added_author = book.added_author ? book.added_author : '';
        object_info.klass = book.klass ? book.klass : [];
        object_info.subjects = book.subjects ? book.subjects : [];

        $.getJSON('//biblionaut.net/services/content.php?callback=?&isbn=' + isbn)
                    .success(contentSuccess)
                    .error(contentError);
    }

    function sruItemInfoError(data) {
        console.log('error');
    }

    function contentSuccess(data) {
        if (data.thumb !== undefined) object_info.thumb = data.thumb;
        if (data.short_desc !== undefined) object_info.short_desc = data.short_desc;
        if (data.long_desc !== undefined) object_info.long_desc = data.long_desc;
        if (data.toc !== undefined) object_info.toc = data.toc;
        makeView(object_info);
    }

    function contentError(data) {
        
    }

    function makeView(book) {
        console.log(book);
        $('#msg').html('');
        if (book.thumb) {
            $('#msg').append('<img src="' + book.thumb + '" style="float:right; max-width: 25%;">');
        }
        $('#msg').append('<a href="http://ask.bibsys.no/ask/action/show?pid=' + book.objektid + '&kid=biblio"><strong>' + book.title + ' ' + book.subtitle + '</strong></a> (' + book.year + ')<br />');

        if (book.main_author) {
            $('#msg').append('av ' + book.main_author.name + '<br />');                            
        }
        if (book.added_author !== undefined ) {
            $('#msg').append('Annen forfatter/redaktør: ' + book.added_author + '<br />');
        }
        $('#msg').append(book.pages + '<br />');
        
        if (book.isbn) {
            var url = 'http://no.wikipedia.org/w/index.php?title=Spesial%3ABokkilder&isbn=' + book.isbn;
            $('#msg').append('ISBN: <a href="' + url + '">' + book.isbn + '</a><br />');
        }
        
        if (book.klass !== undefined) {
            var klasses = [];
            for (var i = 0; i < book.klass.length; i++) {
                klasses.push(book.klass[i].kode); 
            }
            $('#msg').append('Klass: ' + klasses.join(', ') + '<br />');
        }
        
        if (book.subjects !== undefined) {
            var subjects = [];
            for (var i = 0; i < book.subjects.length; i++) {
                subjects.push(book.subjects[i].emne + ' <small>('+book.subjects[i].system + ')</small>'); 
            }
            $('#msg').append('Emner: ' + subjects.join(', ') + '<br />');
        }
    }

    function show_object_info() {

        $('button')
            .show()
            .off('click')
            .on('click', function(e) { window.JSInterface.loanBook(strekkode); });

    }

    $(document).ready(function() {
        $('button').hide();
        var strekkode = querystring('strekkode'),
            isbn10, isbn13, isbn;
        if (strekkode.length === 1) {
            strekkode = strekkode[0];
            object_info = {};
            object_info.barcode = strekkode

            var data = { 
                'id': object_info.barcode
            };
            $('#msg').html('Sjekker ' + object_info.barcode + '...<br />');
            $.getJSON('//biblionaut.net/services/getids.php?callback=?', data, function(response) {
                object_info.objektid = response.objektid
                $('#msg').append('Objektid: ' + object_info.objektid + '<br />');
                if (object_info.objektid.length === 9) {
                    $.getJSON('//biblionaut.net/services/sru_iteminfo.php?callback=?&objektid=' + object_info.objektid)
                        .success(sruItemInfoSuccess)
                        .error(sruItemInfoError);
                }
            });
        } else {
            $('#msg').html('Skann en bok du vil vite det du allerede vet om, men som du ikke visste at telefonen din visste noe om!');            
        }
    });

})();
