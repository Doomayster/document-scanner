/**
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package richtercloud.document.scanner.model;

import java.io.Serializable;
import javax.persistence.Embeddable;

/**
 *
 * @author richter
 */
/*
internal implementation notes:
- in order to allow arbitrary vague addresses (e.g. only the city if other
information isn't known), the class has to be an embeddable (otherwise updates
would destroy data if two Companys intially share that same vague address
entity)
*/
@Embeddable
public class Address implements Serializable, Comparable<Address> {
    private static final long serialVersionUID = 1L;
    private String street;
    private String number;
    private String region;
    private String zipcode;
    private String city;
    private String country;
    private String additional;

    protected Address() {
    }

    public Address(String street,
            String number,
            String zipcode,
            String region,
            String city,
            String country) {
        this.street = street;
        this.number = number;
        this.zipcode = zipcode;
        this.region = region;
        this.city = city;
        this.country = country;
    }

    /**
     * @return the street
     */
    public String getStreet() {
        return this.street;
    }

    /**
     * @param street the street to set
     */
    public void setStreet(String street) {
        this.street = street;
    }

    /**
     * @return the number
     */
    public String getNumber() {
        return this.number;
    }

    /**
     * @param number the number to set
     */
    public void setNumber(String number) {
        this.number = number;
    }

    public String getZipcode() {
        return zipcode;
    }

    public void setZipcode(String zipcode) {
        this.zipcode = zipcode;
    }

    /**
     * @return the region
     */
    public String getRegion() {
        return this.region;
    }

    /**
     * @param region the region to set
     */
    public void setRegion(String region) {
        this.region = region;
    }

    /**
     * @return the city
     */
    public String getCity() {
        return this.city;
    }

    /**
     * @param city the city to set
     */
    public void setCity(String city) {
        this.city = city;
    }

    /**
     * @return the country
     */
    public String getCountry() {
        return this.country;
    }

    /**
     * @param country the country to set
     */
    public void setCountry(String country) {
        this.country = country;
    }

    /**
     * @return the additional
     */
    public String getAdditional() {
        return this.additional;
    }

    /**
     * @param additional the additional to set
     */
    public void setAdditional(String additional) {
        this.additional = additional;
    }

    @Override
    public String toString() {
        String regionString = "";
        if(this.getRegion() != null && !this.getRegion().isEmpty()) {
            regionString = String.format(" (%s)", this.getRegion());
        }
        return String.format("%s %s, %s %s%s, %s",
                this.getStreet(),
                this.getNumber(),
                this.getZipcode(),
                this.getCity(),
                regionString,
                this.getCountry());
    }

    @Override
    public int compareTo(Address o) {
        return toString().compareTo(o.toString());
    }
}
