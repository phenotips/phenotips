//
// Code courtesy of  Jorrit Duin
// http://j0rr1t.blogspot.com.es/2013/02/optimal-compression-for-client-side.html
//

(function(global) {

  function StringCompress() {
  }

  StringCompress.prototype.deflate = function(s) {
    var out = '', i, len, val;
    len = s.length;
    if(len < 1 || typeof s !== 'string'){
      return s;
    }
    // Ensure 1 byte chars (0 / 254)
    s = unescape(encodeURIComponent(s));
    if((len % 2) === 1){
      // Ad an extra byte for byte allignment
      // Odd bytes won't fill a 16 bits slot
      s +=String.fromCharCode(0);
    }
    i = 0;
    len = s.length;
    for(; i< len; i+=2){
      val = (s.charCodeAt(i+1) << 8) + (s.charCodeAt(i));
      out += String.fromCharCode(val);
    }
    return out;
  }


  StringCompress.prototype.inflate = function(s) {
    if(s.length < 1 || typeof s !== 'string'){
      return s;
    }
    var n, out = '', high, low, i, len;
    i = 0;
    len = s.length;
    for(; i< len; i++){
      n = s.charCodeAt(i);
      high = n >> 8;
      low = n - (high << 8);
      out += String.fromCharCode(low);
      if(i == len-1 && high == 0){
        // skip byte
      }else{
        out += String.fromCharCode(high);
      }

    }
    return decodeURIComponent(escape(out));
  }

  global.StringCompress = StringCompress;

}(window));
