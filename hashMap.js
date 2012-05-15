function Map()
{
    // members
    this.keyArray = new Array(); // Keys
    this.valArray = new Array(); // Values

    // methods
    this.put = put;
    this.get = get;
    this.size = size;
    this.clear = clear;
    this.keySet = keySet;
    this.valSet = valSet;
    this.showMe = showMe;   // returns a string with all keys and values in map.
    this.findIt = findIt;
    this.remove = remove;
}

function put( key, val )
{
    var elementIndex = this.findIt( key );

    if( elementIndex == (-1) )
    {
        this.keyArray.push( key );
        this.valArray.push( val );
    }
    else
    {
        this.valArray[ elementIndex ] = val;
    }
}

function get( key )
{
    var result = null;
    var elementIndex = this.findIt( key );

    if( elementIndex != (-1) )
    {
        result = this.valArray[ elementIndex ];
    }

    return result;
}

function remove( key )
{
    var result = null;
    var elementIndex = this.findIt( key );

    if( elementIndex != (-1) )
    {
        this.keyArray = this.keyArray.removeAt(elementIndex);
        this.valArray = this.valArray.removeAt(elementIndex);
    }

    return ;
}

function size()
{
    return (this.keyArray.length);
}

function clear()
{
    for( var i = 0; i < this.keyArray.length; i++ )
    {
        this.keyArray.pop(); this.valArray.pop();
    }
}

function keySet()
{
    return (this.keyArray);
}

function valSet()
{
    return (this.valArray);
}

function showMe()
{
    var result = "";

    for( var i = 0; i < this.keyArray.length; i++ )
    {
        result += "Key: " + this.keyArray[ i ] + "\tValues: " + this.valArray[ i ] + "\n";
    }
    return result;
}

function findIt( key )
{
    var result = (-1);

    for( var i = 0; i < this.keyArray.length; i++ )
    {
        if( this.keyArray[ i ] == key )
        {
            result = i;
            break;
        }
    }
    return result;
}

function removeAt( index )
{
    var part1 = this.slice( 0, index);
    var part2 = this.slice( index+1 );

    return( part1.concat( part2 ) );
}
Array.prototype.removeAt = removeAt;