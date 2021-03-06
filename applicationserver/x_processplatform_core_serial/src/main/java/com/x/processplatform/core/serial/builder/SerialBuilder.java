package com.x.processplatform.core.serial.builder;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import com.google.gson.reflect.TypeToken;
import com.x.base.core.container.EntityManagerContainer;
import com.x.base.core.entity.annotation.CheckPersistType;
import com.x.base.core.project.Context;
import com.x.base.core.project.exception.ExceptionWhen;
import com.x.base.core.project.gson.XGsonBuilder;
import com.x.base.core.project.tools.ListTools;
import com.x.organization.core.express.Organization;
import com.x.processplatform.core.entity.content.SerialNumber;
import com.x.processplatform.core.entity.content.SerialNumber_;
import com.x.processplatform.core.entity.content.Work;
import com.x.processplatform.core.entity.element.Process;

public class SerialBuilder {

	private Context context;

	public SerialBuilder(Context context, EntityManagerContainer emc, String processId, String workId)
			throws Exception {
		this.context = context;
		this.emc = emc;
		process = emc.find(processId, Process.class, ExceptionWhen.not_found);
		work = emc.find(workId, Work.class, ExceptionWhen.not_found);
		serial = new Serial();
		this.date = new Date();
	}

	private EntityManagerContainer emc;

	private Process process;

	private Work work;

	private Date date;

	public Serial serial;

	List<Object> itemResults = new ArrayList<>();

	private Type collectionType = new TypeToken<ArrayList<SerialTextureItem>>() {
	}.getType();

	public String concrete() throws Exception {
		StringBuffer buffer = new StringBuffer("");
		String data = process.getSerialTexture();
		if (StringUtils.isNotEmpty(data)) {
			List<SerialTextureItem> list = XGsonBuilder.instance().fromJson(data, collectionType);
			if (!list.isEmpty()) {
				ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
				ScriptEngine scriptEngine = scriptEngineManager.getEngineByName("nashorn");
				scriptEngine.put("serial", this.serial);
				scriptEngine.put("work", this.work);
				scriptEngine.put("process", this.process);
				for (SerialTextureItem o : list) {
					if (!StringUtils.equalsIgnoreCase(o.getKey(), "number")) {
						Object v = scriptEngine.eval(this.wrap(o.getScript()));
						itemResults.add(v);
					} else {
						itemResults.add("");
					}
				}
				for (int i = 0; i < list.size(); i++) {
					SerialTextureItem o = list.get(i);
					if (StringUtils.equalsIgnoreCase(o.getKey(), "number")) {
						Object v = scriptEngine.eval(this.wrap(o.getScript()));
						itemResults.set(i, v);
					}
				}
				for (Object o : itemResults) {
					buffer.append(Objects.toString(o, ""));
				}
				return buffer.toString();
			}
		}
		return buffer.toString();
	}

	private String wrap(String text) {
		String str = "(function(){";
		str += StringUtils.LF;
		str += text;
		str += StringUtils.LF;
		str += "})();";
		return str;
	}

	public class Serial {
		public String text(String str) {
			return str;
		}

		public String year(String format) {
			return DateFormatUtils.format(date, format);
		}

		public String createYear(String format) {
			return DateFormatUtils.format(work.getCreateTime(), format);
		}

		public String month(String format) {
			return DateFormatUtils.format(date, format);
		}

		public String createMonth(String format) {
			return DateFormatUtils.format(work.getCreateTime(), format);
		}

		public String day(String format) {
			return DateFormatUtils.format(date, format);
		}

		public String createDay(String format) {
			return DateFormatUtils.format(work.getCreateTime(), format);
		}

		public String unit() {
			return work.getCreatorUnit();
		}

		public String person() {
			return work.getCreatorPerson();
		}

		public String identity() {
			return work.getCreatorIdentity();
		}

		public String unitAttribute(String name) throws Exception {
			String result = "";
			Organization org = new Organization(context);
			List<String> attributes = org.unitAttribute().listAttributeWithUnitWithName(work.getCreatorUnit(), name);
			if (ListTools.isNotEmpty(attributes)) {
				result = StringUtils.join(attributes, ",");
			}
			return result;
		}

		public String personAttribute(String name) throws Exception {
			String result = "";
			Organization org = new Organization(context);
			List<String> attributes = org.personAttribute().listAttributeWithPersonWithName(work.getCreatorPerson(),
					name);
			if (ListTools.isNotEmpty(attributes)) {
				result = StringUtils.join(attributes, ",");
			}
			return result;
		}

		public String nextSerialNumber(List<Integer> keys, Integer size) throws Exception {
			String name = "";
			for (Integer i : keys) {
				name += itemResults.get(i).toString();
			}
			Integer number = this.nextNumber(name);
			if (size > 0) {
				return String.format("%0" + size + "d", number);
			} else {
				return number.toString();
			}
		}

		private Integer nextNumber(String name) throws Exception {
			Integer serial = 0;
			EntityManager em = emc.beginTransaction(SerialNumber.class);
			CriteriaBuilder cb = em.getCriteriaBuilder();
			CriteriaQuery<SerialNumber> cq = cb.createQuery(SerialNumber.class);
			Root<SerialNumber> root = cq.from(SerialNumber.class);
			Predicate p = cb.equal(root.get(SerialNumber_.process), process.getId());
			p = cb.and(p, cb.equal(root.get(SerialNumber_.name), name));
			cq.select(root).where(p);
			List<SerialNumber> list = em.createQuery(cq).setMaxResults(1).getResultList();
			SerialNumber serialNumber = null;
			if (list.isEmpty()) {
				serialNumber = new SerialNumber();
				serialNumber.setProcess(process.getId());
				serialNumber.setApplication(process.getApplication());
				serialNumber.setName(name);
				serialNumber.setSerial(1);
				emc.persist(serialNumber, CheckPersistType.all);
				serial = 1;
			} else {
				serialNumber = list.get(0);
				serialNumber.setSerial(serialNumber.getSerial() + 1);
				serial = serialNumber.getSerial();
			}
			return serial;
		}
	}

	public class SerialTextureItem {

		private String key;
		private String script;

		public String getKey() {
			return key;
		}

		public void setKey(String key) {
			this.key = key;
		}

		public String getScript() {
			return script;
		}

		public void setScript(String script) {
			this.script = script;
		}
	}
}