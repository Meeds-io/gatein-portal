/**
 * Copyright (C) 2009 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.services.organization.idm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.InvalidNameException;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.services.organization.UserProfile;
import org.exoplatform.services.organization.UserProfileEventListener;
import org.exoplatform.services.organization.UserProfileHandler;
import org.exoplatform.services.organization.UserStatus;
import org.exoplatform.services.organization.impl.UserProfileImpl;
import org.picketlink.idm.api.Attribute;
import org.picketlink.idm.api.IdentitySession;
import org.picketlink.idm.impl.api.SimpleAttribute;

/**
 * @author <a href="mailto:boleslaw.dawidowicz@redhat.com">Boleslaw Dawidowicz</a>
 */
public class UserProfileDAOImpl extends AbstractDAOImpl implements UserProfileHandler {

    private static UserProfile NOT_FOUND = new UserProfileImpl();

    private List<UserProfileEventListener> listeners_;

    public UserProfileDAOImpl(PicketLinkIDMOrganizationServiceImpl orgService, PicketLinkIDMService service) {
        super(orgService, service);
        listeners_ = new ArrayList<UserProfileEventListener>(3);
    }

    public void addUserProfileEventListener(UserProfileEventListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener cannot be null");
        }
        listeners_.add(listener);
    }

    public void removeUserProfileEventListener(UserProfileEventListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener cannot be null");
        }
        listeners_.remove(listener);
    }

    public final UserProfile createUserProfileInstance() {
        return new UserProfileImpl();
    }

    public UserProfile createUserProfileInstance(String userName) {
        return new UserProfileImpl(userName);
    }

    // void createUserProfileEntry(UserProfile up, IdentitySession session) throws Exception
    // {
    // UserProfileData upd = new UserProfileData();
    // upd.setUserProfile(up);
    // session.save(upd);
    // session.flush();
    // cache_.remove(up.getUserName());
    // }

    public void saveUserProfile(UserProfile profile, boolean broadcast) throws Exception {
        // We need to check if userProfile exists, because organization API is limited and it doesn't have separate methods for
        // "creation" and for "update" of user profile :/

        String username = profile.getUserName();
        UserHandler userHandler = this.orgService.getUserHandler();
        User user = userHandler.findUserByName(username, UserStatus.ANY);
        if(user == null) {
            throw new InvalidNameException("User " + username + " not exists");
        }

        boolean isNew = true;
        if (broadcast) {
            UserProfile found = getProfile(profile.getUserName());
            isNew = found == null;
        }

        if (broadcast) {
            preSave(profile, isNew);
        }

        setProfile(profile.getUserName(), profile);

        if (broadcast) {
            postSave(profile, isNew);
        }

    }

    public UserProfile removeUserProfile(String userName, boolean broadcast) throws Exception {
        UserProfile profile = getProfile(userName);

        if (profile != null) {
            try {
                if (broadcast) {
                    preDelete(profile);
                }

                removeProfile(userName, profile);

                if (broadcast) {
                    postDelete(profile);
                }
                return profile;
            } catch (Exception exp) {
                handleException("Exception occured when removing user profile", exp);
            }
        }
        return null;
    }

    public UserProfile findUserProfileByName(String userName) throws Exception {

        org.picketlink.idm.api.User foundUser = null;

        try {
            foundUser = getIdentitySession().getPersistenceManager().findUser(userName);
        } catch (IllegalArgumentException e) {
            // Don't rethrow the exception to be compatible with other Org Service implementations
            log.debug("Can NOT find any user with username is NULL");
        } catch (Exception e) {
            handleException("Identity operation error: ", e);
        }

        if (foundUser == null) {
            return null;
        }

        UserProfile up = getProfile(userName);

        //
        if (up == null) {
            up = NOT_FOUND;
        }

        // Just to avoid to return a shared object between many threads
        // that would not be thread safe nor corrct
        if (up == NOT_FOUND) {

            //Return NULL as TCK suppose
            return null;
        } else {
            return up;
        }
    }

    public Collection findUserProfiles() throws Exception {
        List<UserProfile> profiles = new LinkedList<UserProfile>();

        UserHandler userHandler = this.orgService.getUserHandler();
        //This should find enabled user
        ListAccess<User> users = userHandler.findAllUsers();
        int size = users.getSize();
        for(User u : users.load(0, size)) {
            UserProfile profile = this.getProfile(u.getUserName());
            if(profile != null) {
                profiles.add(profile);
            }
        }

        return profiles;
    }

    private void preSave(UserProfile profile, boolean isNew) throws Exception {
        for (UserProfileEventListener listener : listeners_) {
            listener.preSave(profile, isNew);
        }
    }

    private void postSave(UserProfile profile, boolean isNew) throws Exception {
        for (UserProfileEventListener listener : listeners_) {
            listener.postSave(profile, isNew);
        }
    }

    private void preDelete(UserProfile profile) throws Exception {
        for (UserProfileEventListener listener : listeners_) {
            listener.preDelete(profile);
        }
    }

    private void postDelete(UserProfile profile) throws Exception {
        for (UserProfileEventListener listener : listeners_) {
            listener.postDelete(profile);
        }
    }

    public UserProfile getProfile(String userName) {

        Object u = null;

        try {
            u = getIdentitySession().getPersistenceManager().findUser(userName);
        } catch (IllegalArgumentException e) {
            // Don't rethrow the exception to be compatible with other Org Service implementations
            log.debug("Can NOT find any user with username is NULL");
        } catch (Exception e) {
            // TODO:
            handleException("Identity operation error: ", e);
        }

        if (u == null) {
            return null;
        }

        Map<String, Attribute> attrs = new HashMap();

        try {
            attrs = getIdentitySession().getAttributesManager().getAttributes(userName);
        } catch (Exception e) {
            // TODO:
            handleException("Identity operation error: ", e);
        }

        if (attrs == null || attrs.isEmpty()) {
            return null;
        }

        Map<String, String> filteredAttrs = new HashMap<>();

        for (String key : attrs.keySet()) {
            // Check if attribute is part of User interface data
            String value = null;
            if (!UserDAOImpl.USER_NON_PROFILE_KEYS.contains(key)) {
                if(attrs.get(key).getValues().size() > 1) {
                    value = attrs.get(key).getValues().toString();
                } else if (attrs.get(key).getValue() != null) {
                    value = attrs.get(key).getValue().toString();
                }
              filteredAttrs.put(key, value);
            }

        }

        if (filteredAttrs.isEmpty()) {
            return null;
        }

        UserProfile profile = new UserProfileImpl(userName, filteredAttrs);

        return profile;

    }

    public void setProfile(String userName, UserProfile profile) throws Exception{

        Map<String, String> profileAttrs = profile.getUserInfoMap();

        Set<Attribute> attrs = new HashSet<Attribute>();

        for (Map.Entry<String, String> entry : profileAttrs.entrySet()) {
            String attrValue = entry.getValue();
            // Treat empty strings as null (needed for compatibility with Oracle as Oracle always treats empty strings as null)
            if ("".equals(attrValue)) {
                attrValue = null;
            }
            attrs.add(new SimpleAttribute(entry.getKey(), attrValue));
        }

        Attribute[] attrArray = new Attribute[attrs.size()];
        attrArray = attrs.toArray(attrArray);

        try {
            getIdentitySession().getAttributesManager().updateAttributes(userName, attrArray);
        } catch (Exception e) {
            // TODO:
            handleException("Identity operation error: ", e);
        }

    }

    public void removeProfile(String userName, UserProfile profile) throws Exception{
        Map<String, String> profileAttrs = profile.getUserInfoMap();

        String[] attrKeys = new String[profileAttrs.keySet().size()];

        attrKeys = profileAttrs.keySet().toArray(attrKeys);

        try {
            getIdentitySession().getAttributesManager().removeAttributes(userName, attrKeys);
        } catch (Exception e) {
            // TODO:
            handleException("Identity operation error: ", e);
        }
    }
}
