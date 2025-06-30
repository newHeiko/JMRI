<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet    version="1.0" 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:db="http://docbook.org/ns/docbook"
    xmlns:xi="http://www.w3.org/2001/XInclude"
    >
    <xsl:output method="xml" encoding="utf-8"/>
    
    <xsl:template match="variables">
        <variables>
            <xsl:copy-of select="node()"/>
            
            <variable item="DummyTest xsl" CV="16.0.260">
                <decVal/>
            </variable>
        </variables>
    </xsl:template>
</xsl:stylesheet>
