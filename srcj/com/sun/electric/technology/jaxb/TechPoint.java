//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.0 in JDK 1.6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2007.04.04 at 06:29:19 PM MSD 
//


package com.sun.electric.technology.jaxb;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for TechPoint complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="TechPoint">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="xa" use="required" type="{http://www.w3.org/2001/XMLSchema}double" />
 *       &lt;attribute name="xm" use="required" type="{http://www.w3.org/2001/XMLSchema}double" />
 *       &lt;attribute name="ya" use="required" type="{http://www.w3.org/2001/XMLSchema}double" />
 *       &lt;attribute name="ym" use="required" type="{http://www.w3.org/2001/XMLSchema}double" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TechPoint")
public class TechPoint {

    @XmlAttribute(required = true)
    protected double xa;
    @XmlAttribute(required = true)
    protected double xm;
    @XmlAttribute(required = true)
    protected double ya;
    @XmlAttribute(required = true)
    protected double ym;

    /**
     * Gets the value of the xa property.
     * 
     */
    public double getXa() {
        return xa;
    }

    /**
     * Sets the value of the xa property.
     * 
     */
    public void setXa(double value) {
        this.xa = value;
    }

    /**
     * Gets the value of the xm property.
     * 
     */
    public double getXm() {
        return xm;
    }

    /**
     * Sets the value of the xm property.
     * 
     */
    public void setXm(double value) {
        this.xm = value;
    }

    /**
     * Gets the value of the ya property.
     * 
     */
    public double getYa() {
        return ya;
    }

    /**
     * Sets the value of the ya property.
     * 
     */
    public void setYa(double value) {
        this.ya = value;
    }

    /**
     * Gets the value of the ym property.
     * 
     */
    public double getYm() {
        return ym;
    }

    /**
     * Sets the value of the ym property.
     * 
     */
    public void setYm(double value) {
        this.ym = value;
    }

}
