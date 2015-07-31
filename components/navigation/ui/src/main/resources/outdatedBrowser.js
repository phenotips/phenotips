if (document.all && !document.addeventlistener)
{
    // remove html on page
    document.body.innerhtml = '';


    // make the banner
    var logo = document.createelement("img");
    logo.src = "https://ccm.sickkids.ca/wp-content/uploads/2015/02/phenotips-logo-300x56.png";
    logo.height = "200";

    var group = document.createelement("div");
    group.id = "centered";

    group.appendchild(logo);
    document.body.appendchild(group);

    // add text
    var text = document.createtextnode("it appears you are using internet explorer version 8 or below.\n"
    + "please download a newer version or a different browser to continue using phenotips using the below links:");
    group.appendchild(text);
    var pnode = document.createelement("p");
    pnode.appendchild(text);
    group.appendchild(pnode);

    // add images
    var div1 = document.createelement("div");
    div1.class = "imagecontainer";
    var chrome = document.createelement("img");
    chrome.src = "http://www.google.com/chrome/assets/common/images/chrome_logo_2x.png";

    var link1 = document.createelement("a");
    link1.href = "https://www.google.com/chrome/browser/desktop/index.html";
    link1.appendchild(chrome);
    div1.appendchild(link1);

    var div2 = document.createelement("div");
    div2.class = "imagecontainer";
    var firefox = document.createelement("img");
    firefox.src = "https://mozorg.cdn.mozilla.net/media/img/firefox/new/header-firefox.98d0a02c957f.png";

    var link2 = document.createelement("a");
    link2.href = "https://www.mozilla.org/en-us/firefox/new/";
    link2.appendchild(firefox);
    div2.appendchild(link2);

    var div3 = document.createelement("div");
    div3.class = "imagecontainer";
    var ie = document.createelement("img");
    ie.src = "https://c.s-microsoft.com/en-ca/cmsimages/iebluee_0304_70x70_en_us.png?version=8cada7a1-74f4-ebb3-cce0-af842a79ada8";

    var link3 = document.createelement("a");
    link3.href = "http://windows.microsoft.com/en-ca/internet-explorer/download-ie";
    link3.appendchild(ie);
    div3.appendchild(link3);

    group.appendchild(div1);
    group.appendchild(div2);
    group.appendchild(div3);
}
