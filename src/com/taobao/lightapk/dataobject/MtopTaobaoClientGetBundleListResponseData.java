
package com.taobao.lightapk.dataobject;

import mtopsdk.mtop.domain.IMTOPDataObject;

import java.util.ArrayList;
import java.util.List;


/**
 * API业务对象
 * 
 */
public class MtopTaobaoClientGetBundleListResponseData
    implements IMTOPDataObject
{
    private List<Item> bundleList;

    public List<Item> getBundleList() {
        return bundleList;
    }

    public void setBundleList(List<Item> bundleList) {
        this.bundleList = bundleList;
    }

    public class Item {
        private String name;
        private String packageUrl;
        private String version;
        private long size;
        private String md5;
        private ArrayList<String> dependency;
        private String title;
        private int type;
        public String info;

        public String getInfo() {
			return info;
		}

		public void setInfo(String info) {
			this.info = info;
		}

		public int getType() {
			return type;
		}

		public void setType(int type) {
			this.type = type;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPackageUrl() {
            return packageUrl;
        }

        public void setPackageUrl(String packageUrl) {
            this.packageUrl = packageUrl;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }

        public String getMd5() {
            return md5;
        }

        public void setMd5(String md5) {
            this.md5 = md5;
        }

        public ArrayList<String> getDependency() {
            return dependency;
        }

        public void setDependency(ArrayList<String> dependency) {
            this.dependency = dependency;
        }
    }

}
